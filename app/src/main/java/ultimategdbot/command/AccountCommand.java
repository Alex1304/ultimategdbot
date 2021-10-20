package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.RetryableInteractionException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.Subcommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import jdash.client.exception.GDClientException;
import jdash.common.entity.GDUserProfile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.ImmutableGdLinkedUser;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.Interactions.confirmButtons;
import static ultimategdbot.util.Interactions.deleteFollowupAndPropagate;

@ChatInputCommand(
        name = "account",
        description = "Manage your connection with your Geometry Dash account.",
        subcommands = {
                @Subcommand(
                        name = "status",
                        description = "Shows your account linking status.",
                        listener = AccountCommand.Status.class
                ),
                @Subcommand(
                        name = "link",
                        description = "Link a Geometry Dash account to your Discord account.",
                        listener = AccountCommand.Link.class
                ),
                @Subcommand(
                        name = "unlink",
                        description = "Unlink your Geometry Dash account from your Discord account.",
                        listener = AccountCommand.Unlink.class
                )
        }
)
public final class AccountCommand {

    @RdiService
    public static final class Status implements ChatInputInteractionListener {

        private final DatabaseService db;
        private final GDClient gdClient;
        private final GDCommandCooldown commandCooldown;

        @RdiFactory
        public Status(DatabaseService db, GDClient gdClient, GDCommandCooldown commandCooldown) {
            this.db = db;
            this.gdClient = gdClient;
            this.commandCooldown = commandCooldown;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return db.gdLinkedUserDao().getActiveLink(ctx.user().getId().asLong())
                    .flatMap(linkedUser -> gdClient.getUserProfile(linkedUser.gdUserId()))
                    .map(user -> Tuples.of(true, ctx.translate(Strings.GD, "currently_linked", user.name())))
                    .defaultIfEmpty(Tuples.of(false, ctx.translate(Strings.GD, "not_yet_linked")))
                    .flatMap(function((isLinked, message) -> ctx.event()
                            .createFollowup(ctx.translate(Strings.GD, "link_intro") + "\n\n" + message + "\n" +
                                    (isLinked ? ctx.translate(Strings.GD, "how_to_unlink")
                                            : ctx.translate(Strings.GD, "how_to_link")))))
                    .then();
        }

        @Override
        public Cooldown cooldown() {
            return commandCooldown.get();
        }
    }

    @RdiService
    public static final class Link implements ChatInputInteractionListener {

        private static final int TOKEN_LENGTH = 6;

        private final DatabaseService db;
        private final GDClient gdClient;
        private final EmojiService emoji;
        private final String botGdName;
        private final GDCommandCooldown commandCooldown;
        private final GDUserService userService;
        private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

        @RdiFactory
        public Link(DatabaseService db, GDClient gdClient, EmojiService emoji, ConfigContainer configContainer,
                    GDCommandCooldown commandCooldown, GDUserService userService) {
            this.db = db;
            this.gdClient = gdClient;
            this.emoji = emoji;
            this.botGdName = configContainer.get(UltimateGDBotConfig.class).gd().client().username();
            this.commandCooldown = commandCooldown;
            this.userService = userService;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            return grammar.resolve(ctx.event())
                    .flatMap(args -> userService.stringToUser(ctx, args.gdUsername))
                    .flatMap(gdUser -> {
                        if (gdUser.accountId() == 0) {
                            return Mono.error(new InteractionFailedException(ctx.translate(Strings.GD,
                                    "error_unregistered_user")));
                        }
                        final var authorId = ctx.user().getId().asLong();
                        return db.gdLinkedUserDao().get(authorId)
                                .defaultIfEmpty(ImmutableGdLinkedUser.builder()
                                        .discordUserId(authorId)
                                        .gdUserId(gdUser.accountId())
                                        .isLinkActivated(false)
                                        .build())
                                .filter(not(GdLinkedUser::isLinkActivated))
                                .switchIfEmpty(Mono.error(new InteractionFailedException(
                                        ctx.translate(Strings.GD, "error_already_linked"))))
                                .flatMap(linkedUser -> {
                                    final var token = linkedUser.confirmationToken()
                                            .filter(ct -> linkedUser.gdUserId() == gdUser.accountId())
                                            .orElse(GDUserService.generateAlphanumericToken(TOKEN_LENGTH));
                                    return db.gdLinkedUserDao()
                                            .save(ImmutableGdLinkedUser.copyOf(linkedUser)
                                                    .withGdUserId(gdUser.accountId())
                                                    .withConfirmationToken(token))
                                            .thenReturn(token);
                                })
                                .flatMap(token -> {
                                    final var menuEmbedContent = ctx.translate(Strings.GD, "link_step_1") + '\n' +
                                            ctx.translate(Strings.GD, "link_step_2", botGdName) + '\n' +
                                            ctx.translate(Strings.GD, "link_step_3") + '\n' +
                                            ctx.translate(Strings.GD, "link_step_4") + '\n' +
                                            ctx.translate(Strings.GD, "link_step_5", token) + '\n' +
                                            ctx.translate(Strings.GD, "link_step_6") + '\n';
                                    final var doneId = UUID.randomUUID().toString();
                                    final var cancelId = UUID.randomUUID().toString();
                                    return ctx.event()
                                            .createFollowup(ctx.translate(Strings.GD, "link_request",
                                                    gdUser.name()) + '\n')
                                            .withEmbeds(EmbedCreateSpec.create()
                                                    .withTitle(ctx.translate(Strings.GD, "link_steps"))
                                                    .withDescription(menuEmbedContent))
                                            .withComponents(ActionRow.of(
                                                    Button.success(doneId,
                                                            ctx.translate(Strings.GD, "account_link_done_button")),
                                                    Button.danger(cancelId,
                                                            ctx.translate(Strings.GENERAL, "cancel_button"))))
                                            .map(Message::getId)
                                            .flatMap(messageId -> Mono.firstWithValue(
                                                            ctx.awaitButtonClick(doneId),
                                                            ctx.awaitButtonClick(cancelId))
                                                    .flatMap(clicked -> clicked.equals(doneId) ?
                                                            handleDone(ctx, token, gdUser) : Mono.empty())
                                                    .onErrorResume(RetryableInteractionException.class, e -> ctx.event()
                                                            .createFollowup(e.getMessage())
                                                            .withEphemeral(true)
                                                            .then(Mono.error(e)))
                                                    .retryWhen(Retry.indefinitely()
                                                            .filter(RetryableInteractionException.class::isInstance))
                                                    .timeout(ctx.getAwaitComponentTimeout())
                                                    .then(ctx.event().deleteFollowup(messageId))
                                                    .onErrorResume(deleteFollowupAndPropagate(ctx, messageId)));
                                });
                    }).then();
        }

        @Override
        public List<ApplicationCommandOptionData> options() {
            return grammar.toOptions();
        }

        @Override
        public Cooldown cooldown() {
            return commandCooldown.get();
        }

        private Mono<Void> handleDone(InteractionContext ctx, String token, GDUserProfile user) {
            final var authorId = ctx.user().getId().asLong();
            return db.gdLinkedUserDao().get(authorId)
                    .filter(linkedUser -> !linkedUser.isLinkActivated()
                            && linkedUser.gdUserId() == user.accountId()
                            && linkedUser.confirmationToken().map(token::equals).orElse(false))
                    .switchIfEmpty(Mono.error(new InteractionFailedException(ctx.translate(Strings.GD,
                            "error_link_check_failed"))))
                    .then(ctx.event().createFollowup(ctx.translate(Strings.GD, "checking_messages")))
                    .map(Message::getId)
                    .flatMap(messageId -> gdClient.withCacheDisabled().getPrivateMessages(0)
                            .filter(message -> message.userAccountId() == user.accountId()
                                    && message.subject().equalsIgnoreCase("confirm"))
                            .switchIfEmpty(Mono.error(new RetryableInteractionException(
                                    ctx.translate(Strings.GD, "error_confirmation_not_found"))))
                            .next()
                            .flatMap(message -> gdClient.downloadPrivateMessage((int) message.id()))
                            .filter(messageDownload -> messageDownload.body().equals(token))
                            .switchIfEmpty(Mono.error(new RetryableInteractionException(
                                    ctx.translate(Strings.GD, "error_confirmation_mismatch"))))
                            .then(db.gdLinkedUserDao().confirmLink(authorId))
                            .then(ctx.event().editFollowup(messageId)
                                    .withContentOrNull(emoji.get("success") + ' ' +
                                            ctx.translate(Strings.GD, "link_success", user.name())))
                            .onErrorMap(GDClientException.class, e -> new RetryableInteractionException(
                                    ctx.translate(Strings.GD, "error_pm_access")))
                            .onErrorResume(deleteFollowupAndPropagate(ctx, messageId))
                            .then());
        }

        private static final class Options {

            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.STRING,
                    name = "gd-username",
                    description = "The username of your GD account.",
                    required = true
            )
            String gdUsername;
        }
    }

    @RdiService
    public static final class Unlink implements ChatInputInteractionListener {

        private final DatabaseService db;
        private final EmojiService emoji;

        @RdiFactory
        public Unlink(DatabaseService db, EmojiService emoji) {
            this.db = db;
            this.emoji = emoji;
        }

        @Override
        public Publisher<?> run(ChatInputInteractionContext ctx) {
            final var authorId = ctx.user().getId().asLong();
            return db.gdLinkedUserDao().getAllActiveLinks(authorId)
                    .switchIfEmpty(Mono.error(new InteractionFailedException(
                            ctx.translate(Strings.GD, "error_not_linked"))))
                    .collectList()
                    .flatMap(linkedUsers -> {
                        if (linkedUsers.size() == 1) {
                            return doUnlink(ctx, List.of(linkedUsers.get(0).discordUserId()));
                        }
                        final var selectId = UUID.randomUUID().toString();
                        return Flux.fromIterable(linkedUsers)
                                .flatMap(linkedUser -> ctx.event().getClient()
                                        .getUserById(Snowflake.of(linkedUser.discordUserId()))
                                        .map(User::getTag)
                                        .onErrorResume(e -> Mono.empty())
                                        .defaultIfEmpty("Unknown User")
                                        .map(tag -> Tuples.of(linkedUser.discordUserId(), tag)))
                                .collectMap(Tuple2::getT1, Tuple2::getT2)
                                .flatMap(tagsByUserId -> ctx.event()
                                        .createFollowup(ctx.translate(Strings.GD, "unlink_select"))
                                        .withComponents(ActionRow.of(SelectMenu.of(selectId, tagsByUserId.entrySet()
                                                        .stream()
                                                        .map(entry -> SelectMenu.Option
                                                                .of(entry.getValue() + " (" + entry.getKey() + ')',
                                                                        entry.getKey() + "")
                                                                .withDefault(entry.getKey() == authorId))
                                                        .collect(Collectors.toUnmodifiableList()))
                                                .withMaxValues(tagsByUserId.size())))
                                        .map(Message::getId)
                                        .flatMap(messageId -> ctx.awaitSelectMenuItems(selectId)
                                                .map(items -> items.stream()
                                                        .map(Snowflake::asLong)
                                                        .collect(Collectors.toUnmodifiableList()))
                                                .flatMap(userIds -> doUnlink(ctx, userIds))
                                                .then(ctx.event().deleteFollowup(messageId))
                                                .onErrorResume(deleteFollowupAndPropagate(ctx, messageId))));
                    })
                    .then();
        }

        private Mono<Void> doUnlink(ChatInputInteractionContext ctx, List<Long> discordUserIds) {
            final var yesId = UUID.randomUUID().toString();
            final var noId = UUID.randomUUID().toString();
            return ctx.event().createFollowup(ctx.translate(Strings.GD, discordUserIds.size() == 1 ?
                            "unlink_confirm" : "unlink_confirm_many"))
                    .withComponents(confirmButtons(ctx, yesId, noId))
                    .map(Message::getId)
                    .flatMap(messageId -> Mono.firstWithValue(ctx.awaitButtonClick(yesId), ctx.awaitButtonClick(noId))
                            .flatMap(clicked -> clicked.equals(noId) ? ctx.event().deleteFollowup(messageId) : Flux
                                    .fromIterable(discordUserIds)
                                    .flatMap(db.gdLinkedUserDao()::delete)
                                    .then(ctx.event().editFollowup(messageId)
                                            .withContentOrNull(emoji.get("success") + ' ' +
                                                    ctx.translate(Strings.GD, discordUserIds.size() == 1 ?
                                                            "unlink_success" : "unlink_success_many"))
                                            .withComponentsOrNull(null))))
                    .then();
        }
    }
}
