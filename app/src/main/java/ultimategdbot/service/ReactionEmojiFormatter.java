package ultimategdbot.service;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.reaction.ReactionEmoji;

@RdiService
public final class ReactionEmojiFormatter {

    private final EmojiService emoji;

    @RdiFactory
    public ReactionEmojiFormatter(EmojiService emoji) {
        this.emoji = emoji;
    }

    public String format(ReactionEmoji reactionEmoji) {
        return reactionEmoji.asUnicodeEmoji()
                .map(ReactionEmoji.Unicode::getRaw)
                .or(() -> reactionEmoji.asCustomEmoji()
                        .map(ReactionEmoji.Custom::getName)
                        .map(emoji::get))
                .orElseThrow();
    }
}
