package ultimategdbot.util;

import botrino.api.i18n.Translator;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.util.MessagePaginator;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;

import java.util.function.Function;

public final class Interactions {

    private Interactions() {
        throw new AssertionError();
    }

    public static ActionRow paginationButtons(Translator tr, MessagePaginator.State state) {
        return ActionRow.of(
                state.previousButton(customId -> Button.secondary(customId,
                        "<< " + tr.translate(Strings.GENERAL, "pagination_previous"))),
                state.nextButton(customId -> Button.secondary(customId,
                        tr.translate(Strings.GENERAL, "pagination_next") + " >>")),
                state.closeButton(customId -> Button.danger(customId,
                        tr.translate(Strings.GENERAL, "pagination_close")))
        );
    }

    public static ActionRow paginationAndConfirmButtons(Translator tr, MessagePaginator.State state, String okId, String cancelId) {
        return ActionRow.of(
                state.previousButton(customId -> Button.secondary(customId,
                        "<< " + tr.translate(Strings.GENERAL, "pagination_previous"))),
                state.nextButton(customId -> Button.secondary(customId,
                        tr.translate(Strings.GENERAL, "pagination_next") + " >>")),
                Button.success(okId, tr.translate(Strings.GENERAL, "ok_button")).disabled(!state.isActive()),
                Button.danger(cancelId, tr.translate(Strings.GENERAL, "cancel_button")).disabled(!state.isActive())
        );
    }

    public static ActionRow confirmButtons(Translator tr, String okId, String cancelId, boolean disabled) {
        return ActionRow.of(
                Button.success(okId, tr.translate(Strings.GENERAL, "ok_button")).disabled(disabled),
                Button.danger(cancelId, tr.translate(Strings.GENERAL, "cancel_button")).disabled(disabled)
        );
    }

    public static ActionRow confirmButtons(Translator tr, String yesId, String noId) {
        return confirmButtons(tr, yesId, noId, false);
    }

    public static <T> Function<Throwable, Mono<T>> deleteFollowupAndPropagate(InteractionContext ctx,
                                                                              Snowflake messageId) {
        return t -> ctx.event().deleteFollowup(messageId)
                .onErrorResume(t2 -> Mono.fromRunnable(() -> t.addSuppressed(t2)))
                .then(Mono.error(t));
    }
}
