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
@JsonSerialize(as = ImmutableGDLinkedUser.class)
@JsonDeserialize(as = ImmutableGDLinkedUser.class)
public interface GDLinkedUser {

    @Criteria.Id
    @JsonProperty("_id")
	long discordUserId();
	
	long gdUserId();
	
	boolean isLinkActivated();
	
	Optional<String> confirmationToken();
}
