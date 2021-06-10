package ultimategdbot.framework;

import botrino.api.util.DurationUtils;
import botrino.api.util.MatcherFunction;
import botrino.command.CommandContext;
import botrino.command.CommandErrorHandler;
import botrino.command.CommandFailedException;
import botrino.command.InvalidSyntaxException;
import botrino.command.cooldown.CooldownException;
import botrino.command.privilege.PrivilegeException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
import discord4j.rest.http.client.ClientException;
import jdash.client.exception.GDClientException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.retry.RetryExhaustedException;
import ultimategdbot.Strings;
import ultimategdbot.exception.BotAdminPrivilegeException;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.ElderModPrivilegeException;
import ultimategdbot.exception.GuildAdminPrivilegeException;
import ultimategdbot.service.EmojiService;

@RdiService
public class UltimateGDBotCommandErrorHandler implements CommandErrorHandler {

    private final EmojiService emoji;
    private final ApplicationInfo applicationInfo;

    @RdiFactory
    public UltimateGDBotCommandErrorHandler(EmojiService emoji, ApplicationInfo applicationInfo) {
        this.emoji = emoji;
        this.applicationInfo = applicationInfo;
    }

    @Override
    public Mono<Void> handleCommandFailed(CommandFailedException e, CommandContext ctx) {
        return sendErrorMessage(ctx, e.getMessage());
    }

    @Override
    public Mono<Void> handlePrivilege(PrivilegeException e, CommandContext ctx) {
        return MatcherFunction.<Mono<Void>>create()
                .matchType(BotOwnerPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GENERAL, "error_privilege_bot_owner")))
                .matchType(BotAdminPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GENERAL, "error_privilege_bot_admin")))
                .matchType(GuildAdminPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GENERAL, "error_privilege_guild_admin")))
                .matchType(ElderModPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GD, "error_privilege_elder_mod")))
                .apply(e)
                .orElse(sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_privilege_generic")));
    }

    @Override
    public Mono<Void> handleInvalidSyntax(InvalidSyntaxException e, CommandContext ctx) {
        final var badArgName = e.getBadArgumentName().orElse(null);
        final var badArgValue = e.getBadArgumentValue().orElse(null);
        String message;
        if (badArgName == null && badArgValue == null) {
            message = ctx.translate(Strings.GENERAL, "subcommand_expected");
        } else if (badArgName == null) {
            message = ctx.translate(Strings.GENERAL, "subcommand_not_found", badArgValue);
        } else if (badArgValue == null) {
            message = ctx.translate(Strings.GENERAL, "missing_argument", badArgName);
        } else {
            message = ctx.translate(Strings.GENERAL, "value_invalid", badArgValue, badArgName);
        }
        return sendErrorMessage(ctx, message);
    }

    @Override
    public Mono<Void> handleDefault(Throwable t, CommandContext ctx) {
        return MatcherFunction.<Mono<Void>>create()
                .matchType(GDClientException.class, e -> sendErrorMessage(ctx, MatcherFunction.<String>create()
                        .matchType(Sinks.EmissionException.class, __ ->
                                ctx.translate(Strings.GD, "error_queue_full"))
                        .matchType(RetryExhaustedException.class, __ ->
                                ctx.translate(Strings.GD, "error_retry_exhausted"))
                        .apply(e.getCause())
                        .orElse(ctx.translate(Strings.GD, "error_server", e.getCause().getMessage()))))
                .matchType(ClientException.class, ClientException.isStatusCode(403), e -> ctx.author()
                        .getPrivateChannel()
                        .flatMap(channel -> channel.createMessage(
                                ctx.translate(Strings.GENERAL, "dm_missing_permissions",
                                        ctx.channel().getMention())))
                        .onErrorResume(__ -> Mono.empty())
                        .then())
                .apply(t)
                .orElse(sendCrashReport(ctx, t)
                        .onErrorMap(e -> {
                            t.addSuppressed(e);
                            return t;
                        })
                        .then(Mono.error(t)));
    }

    @Override
    public Mono<Void> handleCooldown(CooldownException e, CommandContext ctx) {
        return sendErrorMessage(ctx,  ctx.translate(Strings.GD, "error_cooldown", e.getPermits(),
                DurationUtils.format(e.getResetInterval()), DurationUtils.format(e.getRetryAfter().withNanos(0))));
    }

    private Mono<Void> sendErrorMessage(CommandContext ctx, String message) {
        return ctx.channel().createMessage(emoji.get("cross") + " " + message).onErrorResume(e -> Mono.empty()).then();
    }

    private Mono<Void> sendCrashReport(CommandContext ctx, Throwable t) {
        return Mono.when(
                sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_generic")),
                applicationInfo.getOwner().flatMap(User::getPrivateChannel)
                        // That message does not need to be translated as it is only intended for the bot owner.
                        .flatMap(dm -> dm.createMessage("**Something went wrong when executing a command.**\n" +
                                "Input: " + ctx.input().getRaw() + "\n" +
                                "Author: " + ctx.author().getTag() + " (" + ctx.author().getId().asString() + ")\n" +
                                "Exception: `" + t + '`')));
    }
}
