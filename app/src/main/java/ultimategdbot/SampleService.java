package ultimategdbot;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

@RdiService
public final class SampleService {

    private static final Logger LOGGER = Loggers.getLogger(SampleService.class);

    @RdiFactory
    public SampleService(GatewayDiscordClient gateway) {
        gateway.on(ReadyEvent.class, ready -> Mono.fromRunnable(
                        () -> LOGGER.info("Logged in as " + ready.getSelf().getTag())))
                .subscribe();
    }
}
