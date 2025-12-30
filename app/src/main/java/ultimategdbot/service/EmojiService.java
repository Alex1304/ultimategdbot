package ultimategdbot.service;

import botrino.api.util.ApplicationEmojiManager;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@RdiService
public final class EmojiService {

    private final ApplicationEmojiManager emojiManager;

    private EmojiService(ApplicationEmojiManager emojiManager) {
        this.emojiManager = emojiManager;
    }

    @RdiFactory
    public static Mono<EmojiService> create(GatewayDiscordClient gateway) {
        return ApplicationEmojiManager.load(gateway).map(EmojiService::new);
    }

    public String get(String name) {
        try {
            return emojiManager.get(name).asFormat();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Emoji '" + name + "' not found.");
        }
    }

    public ApplicationEmojiManager getEmojiManager() {
        return emojiManager;
    }
}
