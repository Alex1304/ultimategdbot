package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.config.UltimateGDBotConfig;

@RdiService
public final class LegacyCommandRedirectService {

    @RdiFactory
    public LegacyCommandRedirectService(ApplicationInfo applicationInfo, ConfigContainer configContainer,
                                        GatewayDiscordClient gateway, EmojiService emoji) {
        configContainer.get(UltimateGDBotConfig.class).legacyCommandRedirectPrefix()
                .ifPresent(prefix -> gateway.on(MessageCreateEvent.class, event -> {
                    if (event.getMessage().getContent().toLowerCase().startsWith(prefix.toLowerCase())) {
                        return event.getMessage().getChannel()
                                .flatMap(channel -> channel.createMessage(emoji.get("info") + " UltimateGDBot has " +
                                        "moved to Slash Commands. Type `/` in chat, find your command and let the " +
                                        "autocompletion guide you through its usage. If you can't find UltimateGDBot " +
                                        "commands when typing `/`, you may need to ask a server admin to authorize " +
                                        "Slash Commands for this server via this link: " +
                                        "<https://discord.com/oauth2/authorize?client_id=" +
                                        applicationInfo.getId().asString() + "&scope=applications.commands>"));
                    }
                    return Mono.empty();
                }).subscribe());
    }
}
