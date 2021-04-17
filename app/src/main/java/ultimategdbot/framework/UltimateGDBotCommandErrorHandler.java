package ultimategdbot.framework;

import botrino.api.util.EmojiManager;
import botrino.api.util.MatcherFunction;
import botrino.command.CommandContext;
import botrino.command.CommandErrorHandler;
import botrino.command.CommandFailedException;
import botrino.command.privilege.PrivilegeException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.exception.BotAdminPrivilegeException;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.GuildAdminPrivilegeException;

@RdiService
public class UltimateGDBotCommandErrorHandler implements CommandErrorHandler {

    private final EmojiManager emojiManager;

    @RdiFactory
    public UltimateGDBotCommandErrorHandler(EmojiManager emojiManager) {
        this.emojiManager = emojiManager;
    }

    @Override
    public Mono<Void> handleCommandFailed(CommandFailedException e, CommandContext ctx) {
        return sendErrorMessage(ctx, e.getMessage());
    }

    @Override
    public Mono<Void> handlePrivilege(PrivilegeException e, CommandContext ctx) {
        return MatcherFunction.<Mono<Void>>create()
                .matchType(BotOwnerPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.APP, "error_privilege_bot_owner")))
                .matchType(BotAdminPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.APP, "error_privilege_bot_admin")))
                .matchType(GuildAdminPrivilegeException.class, __ -> sendErrorMessage(ctx,
                        ctx.translate(Strings.APP, "error_privilege_guild_admin")))
                .apply(e)
                .orElse(sendErrorMessage(ctx, ctx.translate(Strings.APP, "error_privilege_generic")));
    }

    private Mono<Void> sendErrorMessage(CommandContext ctx, String message) {
        return ctx.channel().createMessage(emojiManager.get("cross").asFormat() + " " + message).then();
    }
}
