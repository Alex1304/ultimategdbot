package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.api.util.EmojiManager;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;
import ultimategdbot.config.UltimateGDBotConfig;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RdiService
public final class EmojiService {

    private final EmojiManager emojiManager;

    private EmojiService(EmojiManager emojiManager) {
        this.emojiManager = emojiManager;
    }

    @RdiFactory
    public static Mono<EmojiService> create(ConfigContainer configContainer, GatewayDiscordClient gateway) {
        var emojiManager = EmojiManager.create(
                configContainer.get(UltimateGDBotConfig.class).emojiGuildIds()
                        .stream()
                        .map(Snowflake::of)
                        .collect(Collectors.toSet()));
        return emojiManager.loadFromGateway(gateway)
                .thenReturn(emojiManager)
                .map(EmojiService::new);
    }

    public String get(String name) {
        try {
            return emojiManager.get(name).asFormat();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Emoji '" + name + "' not found. Make sure the emoji is uploaded in a " +
                    "server that the bot has access to, with the ID of that server listed in `config.json`. After " +
                    "adding the emoji, restart the bot.");
        }
    }

    public EmojiManager getEmojiManager() {
        return emojiManager;
    }
}
