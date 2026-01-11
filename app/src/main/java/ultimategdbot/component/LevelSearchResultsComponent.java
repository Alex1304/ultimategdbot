package ultimategdbot.component;

import botrino.interaction.context.InteractionContext;
import botrino.interaction.util.MessagePaginator;
import discord4j.core.object.component.*;
import discord4j.core.spec.MessageCreateFields;
import jdash.common.Length;
import jdash.common.entity.GDLevel;
import reactor.core.publisher.Flux;
import ultimategdbot.Strings;
import ultimategdbot.service.EmojiService;
import ultimategdbot.util.GDFormatter;

import java.util.ArrayList;
import java.util.List;

import static ultimategdbot.util.GDFormatter.formatCode;
import static ultimategdbot.util.GDLevels.*;

public record LevelSearchResultsComponent(
        InteractionContext ctx,
        EmojiService emoji,
        String title,
        List<? extends GDLevel> levels,
        MessagePaginator.State paginatorState
) implements CustomMessageComponent {

    @Override
    public TopLevelMessageComponent defineComponent() {
        final var components = new ArrayList<ICanBeUsedInContainerComponent>();
        final var header = TextDisplay.of("### " + title);
        final var separator = Separator.of();

        final var pageInfo = TextDisplay.of(String.format("""
                        **%s**
                        %s
                        """,
                ctx.translate(Strings.GENERAL, "page_number", paginatorState.getPage() + 1),
                ctx.translate(Strings.GENERAL, "page_instructions")
        ));
        components.add(header);
        for (final var level : levels) {
            final var signature = difficultySignatureForLevel(level);
            final var title = formatLevelHeader(emoji, level);
            final var coins = formatCoins(emoji, ctx, level);
            final var valueWidth = 5;
            //final var downloadLikesLength = formatDownloadsLikesLength(emoji, level);
            components.add(separator);
            components.add(Section.of(
                    Thumbnail.of(UnfurledMediaItem.of("attachment://" + signature + ".png")),
                    TextDisplay.of(String.format("""
                                    **%s**
                                    %s %s \t %s %s \t %s %s
                                    %s
                                    :musical_note:   %s
                                    **%s %s**
                                    """,
                            title,
                            emoji.get("downloads"),
                            formatCode(GDFormatter.formatHumanReadable(level.downloads()), valueWidth),
                            emoji.get(level.likes() >= 0 ? "like" : "dislike"),
                            formatCode(GDFormatter.formatHumanReadable(level.likes()), valueWidth),
                            emoji.get("length"),
                            formatCode(
                                    level.length() == Length.PLATFORMER ? "PLAT." : level.length().name(), valueWidth),
                            coins,
                            level.song()
                                    .map(s -> formatSong(ctx, s, emoji.get("ncs1") + emoji.get("ncs2")))
                                    .orElse(ctx.translate(Strings.GD, "song_unknown")),
                            ctx.translate(Strings.GD, "label_level_id"),
                            level.id()
                    ))
            ));
        }
        components.add(separator);
        components.add(pageInfo);
        return Container.of(components);
    }

    @Override
    public Flux<? extends MessageCreateFields.File> provideFiles() {
        return Flux.fromIterable(levels)
                .flatMap(result -> getDifficultyImageForLevel(result, true).map(bytes ->
                        MessageCreateFields.File.of(difficultySignatureForLevel(result) + ".png", bytes)));
    }
}
