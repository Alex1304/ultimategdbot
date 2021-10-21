package ultimategdbot.framework;

import botrino.api.util.DurationUtils;
import botrino.api.util.MatcherFunction;
import botrino.interaction.InteractionErrorHandler;
import botrino.interaction.InteractionFailedException;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.cooldown.CooldownException;
import botrino.interaction.privilege.PrivilegeException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
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

import java.util.concurrent.TimeoutException;

@RdiService
public class UGDBErrorHandler implements InteractionErrorHandler {

    private final EmojiService emoji;
    private final ApplicationInfo applicationInfo;

    @RdiFactory
    public UGDBErrorHandler(EmojiService emoji, ApplicationInfo applicationInfo) {
        this.emoji = emoji;
        this.applicationInfo = applicationInfo;
    }

    @Override
    public Mono<Void> handleInteractionFailed(InteractionFailedException e, InteractionContext ctx) {
        return sendErrorMessage(ctx, e.getMessage());
    }

    @Override
    public Mono<Void> handlePrivilege(PrivilegeException e, InteractionContext ctx) {
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
    public Mono<Void> handleDefault(Throwable t, InteractionContext ctx) {
        return MatcherFunction.<Mono<Void>>create()
                .matchType(GDClientException.class, e -> sendErrorMessage(ctx, MatcherFunction.<String>create()
                        .matchType(Sinks.EmissionException.class, __ ->
                                ctx.translate(Strings.GD, "error_queue_full"))
                        .matchType(RetryExhaustedException.class, __ ->
                                ctx.translate(Strings.GD, "error_retry_exhausted"))
                        .apply(e.getCause())
                        .orElse(ctx.translate(Strings.GD, "error_server", e.getCause().getMessage()))))
                .matchType(TimeoutException.class, e -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GENERAL, "command_exec_timeout")))
                .apply(t)
                .orElse(sendCrashReport(ctx, t)
                        .onErrorMap(e -> {
                            t.addSuppressed(e);
                            return t;
                        })
                        .then(Mono.error(t)));
    }

    @Override
    public Mono<Void> handleCooldown(CooldownException e, InteractionContext ctx) {
        return sendErrorMessage(ctx, ctx.translate(Strings.GD, "error_cooldown", e.getPermits(),
                DurationUtils.format(e.getResetInterval()), DurationUtils.format(e.getRetryAfter().withNanos(0))));
    }

    private Mono<Void> sendErrorMessage(InteractionContext ctx, String message) {
        return ctx.event().createFollowup(emoji.get("cross") + " " + message)
                .withEphemeral(true)
                .onErrorResume(e -> Mono.empty()).then();
    }

    private Mono<Void> sendCrashReport(InteractionContext ctx, Throwable t) {
        return Mono.when(
                sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_generic")),
                applicationInfo.getOwner().flatMap(User::getPrivateChannel)
                        // That message does not need to be translated as it is only intended for the bot owner.
                        .flatMap(dm -> dm.createMessage("**Something went wrong when executing a command.**\n" +
                                "Exception: `" + t + '`' + "\n" +
                                "Context:\n```\n" + ctx.toString()
                                .substring(0, Math.min(ctx.toString().length(), 1800)) + "\n```")));
    }
}
