package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.Id;
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
                .id(Id.of(interaction.getId().asLong()))
                .userLocale(interaction.getUserLocale())
                .guildLocale(interaction.getGuildLocale())
                .date(interaction.getId().getTimestamp())
                .guildId(interaction.getGuildId().map(Snowflake::asLong).map(Id::of))
                .channelId(Id.of(interaction.getChannelId().asLong()))
                .user(interaction.getUser().getUserData())
                .member(interaction.getMember().map(Member::getMemberData))
                .command(interaction.getData().data().toOptional())
                .build());
    }
}
