package ultimategdbot.service;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;

public final class ExternalServices {

    private ExternalServices() {
        throw new AssertionError();
    }

    public static Mono<ApplicationInfo> applicationInfo(GatewayDiscordClient gateway) {
        return gateway.getApplicationInfo();
    }
}
