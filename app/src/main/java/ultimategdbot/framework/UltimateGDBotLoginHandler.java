package ultimategdbot.framework;

import botrino.api.config.ConfigContainer;
import botrino.api.config.LoginHandler;
import botrino.api.config.object.BotConfig;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.util.AllowedMentions;
import reactor.core.publisher.Mono;

public final class UltimateGDBotLoginHandler implements LoginHandler {

    @Override
    public Mono<GatewayDiscordClient> login(ConfigContainer configContainer) {
        final var config = configContainer.get(BotConfig.class);
        final var discordClient = DiscordClient.builder(config.token())
                .setDefaultAllowedMentions(AllowedMentions.suppressAll())
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .build();
        return discordClient.gateway()
                .setInitialPresence(shard -> config.presence()
                        .map(BotConfig.StatusConfig::toPresence)
                        .orElseGet(ClientPresence::online))
                .setEnabledIntents(config.enabledIntents().stream().boxed()
                        .map(IntentSet::of)
                        .findAny()
                        .orElseGet(IntentSet::nonPrivileged))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .login()
                .single();
    }
}
