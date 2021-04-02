package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.reactor.ReactorReadable;
import org.immutables.criteria.reactor.ReactorWritable;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableGuildConfig.class)
@JsonDeserialize(as = ImmutableGuildConfig.class)
public interface GuildConfig {

    @Criteria.Id
    @JsonProperty("_id")
    long guildId();

    Optional<String> prefix();

    Optional<String> locale();
}
