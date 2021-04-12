package ultimategdbot.framework;

import botrino.command.CommandContext;
import botrino.command.CommandErrorHandler;
import botrino.command.CommandFailedException;
import reactor.core.publisher.Mono;

public class UltimateGDBotCommandErrorHandler implements CommandErrorHandler {

    @Override
    public Mono<Void> handleCommandFailed(CommandFailedException e, CommandContext ctx) {
        return ctx.channel().createMessage(":no_entry_sign: " + e.getMessage()).then();
    }
}
