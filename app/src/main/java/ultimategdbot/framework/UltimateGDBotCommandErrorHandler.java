package ultimategdbot.framework;

import botrino.api.util.EmojiManager;
import botrino.command.CommandContext;
import botrino.command.CommandErrorHandler;
import botrino.command.CommandFailedException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;

@RdiService
public class UltimateGDBotCommandErrorHandler implements CommandErrorHandler {

    private final EmojiManager emojiManager;

    @RdiFactory
    public UltimateGDBotCommandErrorHandler(EmojiManager emojiManager) {
        this.emojiManager = emojiManager;
    }

    @Override
    public Mono<Void> handleCommandFailed(CommandFailedException e, CommandContext ctx) {
        return ctx.channel().createMessage(emojiManager.get("cross").asFormat() + " " + e.getMessage()).then();
    }
}
