package ultimategdbot.util;

import botrino.command.CommandContext;
import botrino.command.menu.UnexpectedReplyException;
import jdash.client.GDClient;
import reactor.core.publisher.Mono;

public final class InteractionUtils {

    private InteractionUtils() {
        throw new AssertionError();
    }

    public static <T> Mono<T> unexpectedReply(CommandContext ctx, String errorMessage) {
        return ctx.channel().createMessage(errorMessage).then(Mono.error(UnexpectedReplyException::new));
    }

    public static GDClient writeOnlyIfRefresh(CommandContext ctx, GDClient gdClient) {
        return ctx.input().getFlag("refresh").isPresent() ? gdClient.withWriteOnlyCache() : gdClient;
    }
}
