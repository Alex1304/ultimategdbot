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
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.User;
import jdash.client.exception.GDClientException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.retry.RetryExhaustedException;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.Strings;
import ultimategdbot.exception.*;
import ultimategdbot.service.EmojiService;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static java.util.stream.Collectors.joining;

@RdiService
public class UGDBErrorHandler implements InteractionErrorHandler {

    private static final Logger LOGGER = Loggers.getLogger(UGDBErrorHandler.class);

    private final EmojiService emoji;
    private final ApplicationInfo applicationInfo;

    @RdiFactory
    public UGDBErrorHandler(EmojiService emoji, ApplicationInfo applicationInfo) {
        this.emoji = emoji;
        this.applicationInfo = applicationInfo;
    }

    private static String formatContext(InteractionContext ctx) {
        final String commandName;
        if (ctx.event() instanceof ChatInputInteractionEvent) {
            final var event = ((ChatInputInteractionEvent) ctx.event());
            commandName = "/" + event.getCommandName() + (event.getOptions().isEmpty() ? "" :
                    ' ' + formatOptions(event.getOptions()));
        } else if (ctx.event() instanceof ApplicationCommandInteractionEvent) {
            final var event = ((ApplicationCommandInteractionEvent) ctx.event());
            commandName = "[context menu] " + event.getCommandName();
        } else if (ctx.event() instanceof ComponentInteractionEvent) {
            commandName = "[component] " + ((ComponentInteractionEvent) ctx.event()).getCustomId();
        } else {
            commandName = "unknown";
        }
        return "Command: " + commandName + "\n" +
                "User: " + ctx.event().getInteraction().getUser().getTag() + "\n";
    }

    private static String formatOptions(List<ApplicationCommandInteractionOption> options) {
        return options.stream()
                .map(o -> o.getType() == ApplicationCommandOption.Type.SUB_COMMAND ||
                        o.getType() == ApplicationCommandOption.Type.SUB_COMMAND_GROUP ?
                        o.getName() + ' ' + formatOptions(o.getOptions()) : formatOptionValue(o))
                .collect(joining(" "));
    }

    private static String formatOptionValue(ApplicationCommandInteractionOption o) {
        return o.getName() + ":" + o.getValue()
                .map(ApplicationCommandInteractionOptionValue::getRaw)
                .orElse("<null>");
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
                .matchType(ManageWebhooksPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GENERAL, "error_privilege_manage_webhooks")))
                .matchType(ElderModPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GD, "error_privilege_elder_mod")))
                .apply(e)
                .orElse(sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_privilege_generic")));
    }

    @Override
    public Mono<Void> handleDefault(Throwable t, InteractionContext ctx) {
        return MatcherFunction.<Mono<Void>>create()
                .matchType(GDClientException.class, e -> {
                    LOGGER.error("Error executing request to GD servers", e);
                    return sendErrorMessage(ctx, MatcherFunction.<String>create()
                            .matchType(Sinks.EmissionException.class, __ ->
                                    ctx.translate(Strings.GD, "error_queue_full"))
                            .matchType(RetryExhaustedException.class, __ ->
                                    ctx.translate(Strings.GD, "error_retry_exhausted"))
                            .apply(e.getCause())
                            .orElse(ctx.translate(Strings.GD, "error_server", e.getCause().getMessage())));
                })
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
        final var format = formatContext(ctx);
        return Mono.when(
                sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_generic")),
                applicationInfo.getOwner().flatMap(User::getPrivateChannel)
                        // That message does not need to be translated as it is only intended for the bot owner.
                        .flatMap(dm -> dm.createMessage("**Something went wrong when executing a command.**\n" +
                                "Exception: `" + t + '`' + "\n" +
                                "Context:\n```\n" + format.substring(0, Math.min(format.length(), 1500)) +
                                "\n```")));
    }
}
