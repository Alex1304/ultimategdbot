package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import jdash.client.GDClient;
import jdash.client.cache.GDCache;
import jdash.client.request.GDRouter;
import jdash.client.request.RequestLimiter;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.config.UltimateGDBotConfig;

import java.time.Duration;

public final class ExternalServices {

    private static final Logger LOGGER = Loggers.getLogger(ExternalServices.class);

    private ExternalServices() {
        throw new AssertionError();
    }

    public static Mono<ApplicationInfo> applicationInfo(GatewayDiscordClient gateway) {
        return gateway.getApplicationInfo();
    }

    public static Mono<GDClient> gdClient(ConfigContainer configContainer) {
        var config = configContainer.get(UltimateGDBotConfig.class).gd().client();
        return GDClient.create()
                .withRouter(GDRouter.builder()
                        .setBaseUrl(config.host())
                        .setRequestTimeout(config.requestTimeoutSeconds() > 0
                                ? Duration.ofSeconds(config.requestTimeoutSeconds()) : null)
                        .setRequestLimiter(config.requestLimiter()
                                .map(l -> RequestLimiter.of(l.limit(), Duration.ofSeconds(l.intervalSeconds())))
                                .orElseGet(RequestLimiter::none))
                        .build())
                .withCache(GDCache.caffeine(c -> c.expireAfterAccess(Duration.ofSeconds(config.cacheTtlSeconds()))))
                .login(config.username(), config.password())
                .doOnNext(client -> LOGGER.debug("Successfully logged into GD account " + config.username()));
    }
}
