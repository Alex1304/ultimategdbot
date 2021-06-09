package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.reactor.ReactorReadable;
import org.immutables.criteria.reactor.ReactorWritable;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableGdAwardedLevel.class)
@JsonDeserialize(as = ImmutableGdAwardedLevel.class)
public interface GdAwardedLevel {

    @Criteria.Id
    @JsonProperty("_id")
	long levelId();
	
	Instant insertDate();
	
	int downloads();
	
	int likes();
}
