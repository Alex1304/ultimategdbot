package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.api.util.EmojiManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.config.UltimateGDBotConfig;

import java.util.stream.Collectors;

public final class ExternalServices {

    private ExternalServices() {
        throw new AssertionError();
    }

    public static Mono<ApplicationInfo> applicationInfo(GatewayDiscordClient gateway) {
        return gateway.getApplicationInfo();
    }

    public static Mono<EmojiManager> emojiManager(ConfigContainer configContainer, GatewayDiscordClient gateway) {
        var emojiManager = EmojiManager.create(
                configContainer.get(UltimateGDBotConfig.class).emojiGuildIds()
                        .stream()
                        .map(Snowflake::of)
                        .collect(Collectors.toSet()));
        return emojiManager.loadFromGateway(gateway).thenReturn(emojiManager);
    }
}
