package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.interaction.context.InteractionContext;
import botrino.interaction.util.MessagePaginator;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.util.Interactions;

import java.util.List;
import java.util.function.UnaryOperator;

@RdiService
public final class OutputPaginator {

    private final int paginationMaxEntries;

    @RdiFactory
    public OutputPaginator(ConfigContainer configContainer) {
        var config = configContainer.get(UltimateGDBotConfig.class);
        this.paginationMaxEntries = config.paginationMaxEntries();
    }

    public Mono<Void> paginate(InteractionContext ctx, List<String> list, UnaryOperator<String> contentTransformer) {
        if (list.isEmpty()) {
            return ctx.event()
                    .createFollowup(contentTransformer.apply(ctx.translate(Strings.GENERAL, "no_data")))
                    .then();
        }
        if (list.size() <= paginationMaxEntries) {
            return ctx.event()
                    .createFollowup(contentTransformer.apply(String.join("\n", list)))
                    .then();
        }
        final var pageCount = list.size() / paginationMaxEntries + 1;
        return MessagePaginator.paginate(ctx, pageCount, state -> Mono.just(MessageCreateSpec.create()
                .withContent(contentTransformer.apply(
                        String.join("\n", list.subList(state.getPage() * paginationMaxEntries,
                                Math.min(list.size(), (state.getPage() + 1) * paginationMaxEntries)))))
                .withEmbeds(EmbedCreateSpec.create().withFields(Field.of(
                        ctx.translate(Strings.GENERAL, "page_x", (state.getPage() + 1), pageCount),
                        ctx.translate(Strings.GENERAL, "page_instructions"), false)))
                .withComponents(Interactions.paginationButtons(ctx, state))));
    }

    public Mono<Void> paginate(InteractionContext ctx, List<String> list) {
        return paginate(ctx, list, Object::toString);
    }
}
