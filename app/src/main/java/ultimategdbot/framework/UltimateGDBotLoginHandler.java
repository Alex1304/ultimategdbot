package ultimategdbot.framework;

import botrino.api.config.ConfigContainer;
import botrino.api.config.LoginHandler;
import botrino.api.config.object.BotConfig;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.route.Routes;
import discord4j.rest.util.AllowedMentions;
import reactor.core.publisher.Mono;
import ultimategdbot.exception.CreateMessage500Exception;

import java.util.function.Function;

public final class UltimateGDBotLoginHandler implements LoginHandler {

    @Override
    public Mono<GatewayDiscordClient> login(ConfigContainer configContainer) {
        final var config = configContainer.get(BotConfig.class);
        final var discordClient = DiscordClient.builder(config.token())
                .setDefaultAllowedMentions(AllowedMentions.suppressAll())
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .onClientResponse(request -> {
                    if (RouteMatcher.route(Routes.MESSAGE_CREATE).matches(request)) {
                        return mono -> mono.onErrorMap(ClientException.isStatusCode(500, 502, 503, 504, 520),
                                e -> new CreateMessage500Exception());
                    }
                    if (RouteMatcher.route(Routes.WEBHOOK_MESSAGE_EDIT).matches(request)) {
                        return mono -> mono.onErrorResume(ClientException.isStatusCode(401), e -> Mono.empty());
                    }
                    if (RouteMatcher.route(Routes.INTERACTION_RESPONSE_CREATE).matches(request)) {
                        return mono -> mono.onErrorResume(ClientException.isStatusCode(400), e -> Mono.empty());
                    }
                    return Function.identity();
                })
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
