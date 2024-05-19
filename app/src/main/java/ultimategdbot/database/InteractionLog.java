package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
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
	long id();

    Instant date();

    String locale();

    Optional<ApplicationCommandInteractionData> commandData();
}
