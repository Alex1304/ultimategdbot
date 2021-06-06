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
@JsonSerialize(as = ImmutableGDLeaderboard.class)
@JsonDeserialize(as = ImmutableGDLeaderboard.class)
public interface GDLeaderboard {

    @Criteria.Id
    @JsonProperty("_id")
	long accountId();
	
	String name();
	
	int stars();
	
	int diamonds();
	
	int userCoins();
	
	int secretCoins();
	
	int demons();
	
	int creatorPoints();
	
	Instant lastRefreshed();
}
