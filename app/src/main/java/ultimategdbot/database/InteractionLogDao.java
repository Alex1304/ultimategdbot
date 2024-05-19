package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.Interaction;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

@RdiService
public final class InteractionLogDao {

    private final InteractionLogRepository repository;

    @RdiFactory
    public InteractionLogDao(DatabaseService db) {
        this.repository = new InteractionLogRepository(db.getBackend());
    }

    public Mono<WriteResult> saveInteraction(Interaction interaction) {
        if (interaction.getCommandInteraction().isEmpty()) {
            return Mono.empty();
        }
        return repository.upsert(ImmutableInteractionLog.builder()
                .id(interaction.getId().asLong())
                .locale(interaction.getGuildLocale().orElse(interaction.getUserLocale()))
                .date(interaction.getId().getTimestamp())
                .commandData(interaction.getData().data().toOptional())
                .build());
    }
}
