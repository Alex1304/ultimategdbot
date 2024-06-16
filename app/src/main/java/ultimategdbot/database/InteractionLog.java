package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.MemberData;
import discord4j.discordjson.json.UserData;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.reactor.ReactorReadable;
import org.immutables.criteria.reactor.ReactorWritable;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableInteractionLog.class)
@JsonDeserialize(as = ImmutableInteractionLog.class)
public interface InteractionLog {

    @Criteria.Id
    @JsonProperty("_id")
    Id id();

    Instant date();

    String userLocale();

    Optional<String> guildLocale();

    Optional<Id> guildId();

    Optional<MemberData> member();

    UserData user();

    Id channelId();

    Optional<ApplicationCommandInteractionData> command();
}
