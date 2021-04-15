package ultimategdbot.service;

import botrino.api.util.EmojiManager;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.GuildEmoji;
import discord4j.core.object.reaction.ReactionEmoji;

@RdiService
public final class ReactionEmojiFormatter {

    private final EmojiManager emojiManager;

    @RdiFactory
    public ReactionEmojiFormatter(EmojiManager emojiManager) {
        this.emojiManager = emojiManager;
    }

    public String format(ReactionEmoji reactionEmoji) {
        return reactionEmoji.asUnicodeEmoji()
                .map(ReactionEmoji.Unicode::getRaw)
                .or(() -> reactionEmoji.asCustomEmoji()
                        .map(ReactionEmoji.Custom::getName)
                        .map(emojiManager::get)
                        .map(GuildEmoji::asFormat))
                .orElseThrow();
    }
}
