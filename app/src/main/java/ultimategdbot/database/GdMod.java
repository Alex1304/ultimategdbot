package ultimategdbot.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.criteria.reactor.ReactorReadable;
import org.immutables.criteria.reactor.ReactorWritable;
import org.immutables.value.Value;

@Value.Immutable
@Criteria
@Criteria.Repository(facets = { ReactorReadable.class, ReactorWritable.class })
@JsonSerialize(as = ImmutableGdMod.class)
@JsonDeserialize(as = ImmutableGdMod.class)
public interface GdMod {

    @Criteria.Id
    @JsonProperty("_id")
	long accountId();
	
	String name();

	int elder();
	
	default boolean isElder() {
	    return elder() > 0;
    }
}
