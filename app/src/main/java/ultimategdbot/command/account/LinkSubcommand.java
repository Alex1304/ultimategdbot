package ultimategdbot.command.account;

import botrino.api.config.ConfigContainer;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.RetryableInteractionException;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import jdash.client.exception.GDClientException;
import jdash.common.entity.GDUserProfile;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.database.ImmutableGdLinkedUser;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDUserService;

import java.util.List;
import java.util.UUID;

import static java.util.function.Predicate.not;
import static ultimategdbot.util.Interactions.deleteFollowupAndPropagate;

@RdiService
public final class LinkSubcommand implements ChatInputInteractionListener {

    private static final int TOKEN_LENGTH = 6;

    private final GdLinkedUserDao gdLinkedUserDao;
    private final GDClient gdClient;
    private final EmojiService emoji;
    private final String botGdName;
    private final GDCommandCooldown commandCooldown;
    private final GDUserService userService;
    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public LinkSubcommand(GdLinkedUserDao gdLinkedUserDao, GDClient gdClient, EmojiService emoji,
                          ConfigContainer configContainer, GDCommandCooldown commandCooldown,
                          GDUserService userService) {
        this.gdLinkedUserDao = gdLinkedUserDao;
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
                .flatMap(profile -> {
                    if (profile.user().accountId() == 0) {
                        return Mono.error(new InteractionFailedException(ctx.translate(Strings.GD,
                                "error_unregistered_user")));
                    }
                    final var authorId = ctx.user().getId().asLong();
                    return gdLinkedUserDao.get(authorId)
                            .defaultIfEmpty(ImmutableGdLinkedUser.builder()
                                    .discordUserId(authorId)
                                    .gdUserId(profile.user().accountId())
                                    .isLinkActivated(false)
                                    .build())
                            .filter(not(GdLinkedUser::isLinkActivated))
                            .switchIfEmpty(Mono.error(new InteractionFailedException(
                                    ctx.translate(Strings.GD, "error_already_linked"))))
                            .flatMap(linkedUser -> {
                                final var token = linkedUser.confirmationToken()
                                        .filter(ct -> linkedUser.gdUserId() == profile.user().accountId())
                                        .orElse(GDUserService.generateAlphanumericToken(TOKEN_LENGTH));
                                return gdLinkedUserDao
                                        .save(ImmutableGdLinkedUser.copyOf(linkedUser)
                                                .withGdUserId(profile.user().accountId())
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
                                                profile.user().name()) + '\n')
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
                                                        handleDone(ctx, token, profile) : Mono.empty())
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

    private Mono<Void> handleDone(InteractionContext ctx, String token, GDUserProfile profile) {
        final var authorId = ctx.user().getId().asLong();
        return gdLinkedUserDao.get(authorId)
                .filter(linkedUser -> !linkedUser.isLinkActivated()
                        && linkedUser.gdUserId() == profile.user().accountId()
                        && linkedUser.confirmationToken().map(token::equals).orElse(false))
                .switchIfEmpty(Mono.error(new InteractionFailedException(ctx.translate(Strings.GD,
                        "error_link_check_failed"))))
                .then(ctx.event().createFollowup(ctx.translate(Strings.GD, "checking_messages")))
                .map(Message::getId)
                .flatMap(messageId -> gdClient.withCacheDisabled().getPrivateMessages(0)
                        .filter(message -> message.userAccountId() == profile.user().accountId()
                                && message.subject().equalsIgnoreCase("confirm"))
                        .switchIfEmpty(Mono.error(new RetryableInteractionException(
                                ctx.translate(Strings.GD, "error_confirmation_not_found"))))
                        .next()
                        .flatMap(message -> gdClient.downloadPrivateMessage((int) message.id()))
                        .filter(messageDownload -> messageDownload.body().equals(token))
                        .switchIfEmpty(Mono.error(new RetryableInteractionException(
                                ctx.translate(Strings.GD, "error_confirmation_mismatch"))))
                        .then(gdLinkedUserDao.confirmLink(authorId))
                        .then(ctx.event().editFollowup(messageId)
                                .withContentOrNull(emoji.get("success") + ' ' +
                                        ctx.translate(Strings.GD, "link_success", profile.user().name())))
                        .onErrorMap(GDClientException.class, e -> new RetryableInteractionException(
                                ctx.translate(Strings.GD, "error_pm_access")))
                        .onErrorResume(deleteFollowupAndPropagate(ctx, messageId))
                        .then());
    }

    private record Options(
            @ChatInputCommandGrammar.Option(
                    type = ApplicationCommandOption.Type.STRING,
                    name = "gd-username",
                    description = "The username of your GD account.",
                    required = true
            )
            String gdUsername
    ) {}
}
