package ultimategdbot.command;

import botrino.api.config.ConfigContainer;
import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.CommandService;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.cooldown.Cooldown;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.CommandGrammar;
import botrino.command.menu.UnexpectedReplyException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import jdash.client.GDClient;
import jdash.client.exception.GDClientException;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.database.GdLinkedUser;
import ultimategdbot.database.ImmutableGdLinkedUser;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.DatabaseService;
import ultimategdbot.service.EmojiService;
import ultimategdbot.service.GDUserService;

import java.util.Set;

import static java.util.function.Predicate.not;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.InteractionUtils.unexpectedReply;

@CommandCategory(CommandCategory.GD)
@Alias("account")
@TopLevelCommand
@RdiService
public final class AccountCommand implements Command {

    private static final int TOKEN_LENGTH = 6;

    private final GDCommandCooldown commandCooldown;
    private final DatabaseService db;
    private final EmojiService emoji;
    private final GDClient gdClient;
    private final String botGdName;
    private final CommandService commandService;
    private final ReactionEmoji reactionCross, reactionSuccess;

    private final CommandGrammar<LinkArgs> linkGrammar;

    @RdiFactory
    public AccountCommand(GDCommandCooldown commandCooldown, DatabaseService db, EmojiService emoji, GDClient gdClient,
                          ConfigContainer configContainer, GDUserService userService, CommandService commandService) {
        this.commandCooldown = commandCooldown;
        this.db = db;
        this.emoji = emoji;
        this.gdClient = gdClient;
        this.botGdName = configContainer.get(UltimateGDBotConfig.class).gd().client().username();
        this.linkGrammar = CommandGrammar.builder()
                .nextArgument("gdUser", userService::stringToUser)
                .build(LinkArgs.class);
        this.commandService = commandService;
        this.reactionCross = commandService.interactiveMenuFactory().getPaginationControls().getCloseEmoji();
        this.reactionSuccess = ReactionEmoji.custom(emoji.getEmojiManager().get("success"));
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return db.gdLinkedUserDao().getActiveLink(ctx.author().getId().asLong())
                .flatMap(linkedUser -> gdClient.getUserProfile(linkedUser.gdUserId()))
                .map(user -> Tuples.of(true, ctx.translate(Strings.GD, "currently_linked", user.name())))
                .defaultIfEmpty(Tuples.of(false, ctx.translate(Strings.GD, "not_yet_linked")))
                .flatMap(function((isLinked, message) -> ctx.channel()
                        .createMessage(ctx.translate(Strings.GD, "link_intro") + "\n\n" + message + "\n" +
                                (isLinked ? ctx.translate(Strings.GD, "how_to_link", ctx.getPrefixUsed())
                                        : ctx.translate(Strings.GD, "how_to_unlink", ctx.getPrefixUsed())))))
                .then();
    }

    private Mono<Void> runLink(CommandContext ctx) {
        return linkGrammar.resolve(ctx).map(args -> args.gdUser).flatMap(gdUser -> {
            if (gdUser.accountId() == 0) {
                return Mono.error(new CommandFailedException(ctx.translate(Strings.GD, "error_unregistered_user")));
            }
            final var authorId = ctx.author().getId().asLong();
            return db.gdLinkedUserDao().get(authorId)
                    .defaultIfEmpty(ImmutableGdLinkedUser.builder()
                            .discordUserId(authorId)
                            .gdUserId(gdUser.accountId())
                            .isLinkActivated(false)
                            .build())
                    .filter(not(GdLinkedUser::isLinkActivated))
                    .switchIfEmpty(Mono.error(new CommandFailedException(
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
                        return commandService.interactiveMenuFactory()
                                .create(MessageCreateSpec.create()
                                        .withContent(ctx.translate(Strings.GD, "link_request", gdUser.name()) + '\n')
                                        .withEmbed(EmbedCreateSpec.create()
                                                .withTitle(ctx.translate(Strings.GD, "link_steps"))
                                                .withDescription(menuEmbedContent)))
                                .addReactionItem(reactionSuccess, interaction -> interaction.getEvent().isAddEvent()
                                        ? handleDone(ctx, token, gdUser)
                                        .then(Mono.<Void>fromRunnable(interaction::closeMenu))
                                        .onErrorResume(UnexpectedReplyException.class, e -> interaction.getMenuMessage()
                                                .removeReaction(interaction.getEvent().getEmoji(), ctx.author().getId())
                                                .onErrorResume(ClientException.isStatusCode(403, 404),
                                                        e0 -> Mono.empty()))
                                        : Mono.empty())
                                .addReactionItem(reactionCross,
                                        interaction -> Mono.fromRunnable(interaction::closeMenu))
                                .deleteMenuOnClose(true)
                                .deleteMenuOnTimeout(true)
                                .closeAfterReaction(false)
                                .open(ctx);
                    });
        }).then();
    }

    private Mono<Void> handleDone(CommandContext ctx, String token, GDUserProfile user) {
        final var authorId = ctx.author().getId().asLong();
        return db.gdLinkedUserDao().get(authorId)
                .filter(linkedUser -> !linkedUser.isLinkActivated()
                        && linkedUser.gdUserId() == user.accountId()
                        && linkedUser.confirmationToken().map(token::equals).orElse(false))
                .switchIfEmpty(Mono.error(new CommandFailedException(ctx.translate(Strings.GD,
                        "error_link_check_failed"))))
                .then(ctx.channel().createMessage(ctx.translate(Strings.GD, "checking_messages")))
                .flatMap(waitMessage -> gdClient.withCacheDisabled().getPrivateMessages(0)
                        .filter(message -> message.userAccountId() == user.accountId()
                                && message.subject().equalsIgnoreCase("confirm"))
                        .switchIfEmpty(unexpectedReply(ctx, ctx.translate(Strings.GD, "error_confirmation_not_found")))
                        .next()
                        .flatMap(message -> gdClient.downloadPrivateMessage((int) message.id()))
                        .filter(messageDownload -> messageDownload.body().equals(token))
                        .switchIfEmpty(unexpectedReply(ctx, ctx.translate(Strings.GD, "error_confirmation_mismatch")))
                        .then(db.gdLinkedUserDao().confirmLink(authorId))
                        .then(ctx.channel().createMessage(emoji.get("success") + ' ' +
                                ctx.translate(Strings.GD, "link_success", user.name())))
                        .onErrorResume(GDClientException.class, e -> unexpectedReply(ctx,
                                ctx.translate(Strings.GD, "error_pm_access")))
                        .doFinally(signal -> waitMessage.delete().subscribe(null, e -> {}))
                        .then());
    }

    private Mono<Void> runUnlink(CommandContext ctx) {
        final var authorId = ctx.author().getId().asLong();
        return db.gdLinkedUserDao().getActiveLink(authorId)
                .switchIfEmpty(Mono.error(new CommandFailedException(
                        ctx.translate(Strings.GD, "error_not_linked"))))
                .flatMap(linkedUser -> commandService.interactiveMenuFactory()
                        .create(ctx.translate(Strings.GD, "unlink_confirm"))
                        .deleteMenuOnClose(true)
                        .deleteMenuOnTimeout(true)
                        .closeAfterReaction(true)
                        .addReactionItem(reactionSuccess, interaction -> db.gdLinkedUserDao().delete(authorId)
                                .then(ctx.channel().createMessage(emoji.get("success") + ' ' +
                                        ctx.translate(Strings.GD, "unlink_success")))
                                .then())
                        .addReactionItem(reactionCross, interaction -> Mono.empty())
                        .open(ctx))
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "account_description"))
                .build();
    }

    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("link", this::runLink)
                        .inheritFrom(this)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setSyntax(linkGrammar.toString())
                                .setDescription(tr.translate(Strings.HELP, "account_link_description"))
                                .setBody(tr.translate(Strings.HELP, "account_link_body"))
                                .build())
                        .build(),
                Command.builder("unlink", this::runUnlink)
                        .inheritFrom(this)
                        .setDocumentation(tr -> CommandDocumentation.builder()
                                .setDescription(tr.translate(Strings.HELP, "account_unlink_description"))
                                .build())
                        .build()
        );
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private static class LinkArgs {
        GDUserProfile gdUser;
    }
}
