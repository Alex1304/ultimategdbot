package ultimategdbot.service;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.Interaction;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.database.InteractionLogDao;

@RdiService
public final class InteractionLogService {

    private static final Logger LOGGER = Loggers.getLogger(InteractionLogService.class);

    private final InteractionLogDao interactionLogDao;
    private final Sinks.Many<Interaction> interactions = Sinks.many().unicast().onBackpressureBuffer();

    @RdiFactory
    public InteractionLogService(InteractionLogDao interactionLogDao) {
        this.interactionLogDao = interactionLogDao;
        run();
    }

    public void log(Interaction interaction) {
        final var result = interactions.tryEmitNext(interaction);
        if (result.isFailure()) {
            LOGGER.warn("Interaction log failed", interaction);
        }
    }

    private void run() {
        interactions.asFlux()
                .flatMap(interaction -> interactionLogDao.saveInteraction(interaction)
                        .onErrorResume(e -> Mono.fromRunnable(() ->
                                LOGGER.error("Error when saving interaction log", e))))
                .subscribe();
        LOGGER.info("Interaction log service running");
    }
}
