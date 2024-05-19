package ultimategdbot.framework;

import botrino.interaction.InteractionEventProcessor;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import reactor.core.publisher.Mono;
import ultimategdbot.service.BlacklistService;
import ultimategdbot.service.InteractionLogService;

@RdiService
public final class UGDBEventProcessor implements InteractionEventProcessor {

    private final BlacklistService blacklist;
    private final InteractionLogService interactionLogService;

    @RdiFactory
    public UGDBEventProcessor(BlacklistService blacklist, InteractionLogService interactionLogService) {
        this.blacklist = blacklist;
        this.interactionLogService = interactionLogService;
    }

    @Override
    public Mono<Boolean> filter(InteractionCreateEvent event) {
        interactionLogService.log(event.getInteraction());
        return Mono.just(blacklist.isBlacklisted(event));
    }
}
