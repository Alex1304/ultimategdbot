package ultimategdbot.framework;

import botrino.api.util.MatcherFunction;
import botrino.command.CommandContext;
import botrino.command.CommandErrorHandler;
import botrino.command.CommandFailedException;
import botrino.command.InvalidSyntaxException;
import botrino.command.privilege.PrivilegeException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.rest.http.client.ClientException;
import jdash.client.exception.GDClientException;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.exception.BotAdminPrivilegeException;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.ElderModPrivilegeException;
import ultimategdbot.exception.GuildAdminPrivilegeException;
import ultimategdbot.service.EmojiService;

@RdiService
public class UltimateGDBotCommandErrorHandler implements CommandErrorHandler {

    private final EmojiService emoji;

    @RdiFactory
    public UltimateGDBotCommandErrorHandler(EmojiService emoji) {
        this.emoji = emoji;
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
                .matchType(GDClientException.class, e -> sendErrorMessage(ctx,
                        ctx.translate(Strings.GD, "error_server", e.getCause().getMessage())))
                .matchType(ClientException.class, ClientException.isStatusCode(403), e -> ctx.author()
                        .getPrivateChannel()
                        .flatMap(channel -> channel.createMessage(
                                ctx.translate(Strings.GENERAL, "dm_missing_permissions",
                                        ctx.channel().getMention())))
                        .then())
                .apply(t)
                .orElse(sendErrorMessage(ctx, ctx.translate(Strings.GENERAL, "error_generic"))
                        .then(Mono.error(t)));
    }

    private Mono<Void> sendErrorMessage(CommandContext ctx, String message) {
        return ctx.channel().createMessage(emoji.get("cross") + " " + message).onErrorResume(e -> Mono.empty()).then();
    }
}
