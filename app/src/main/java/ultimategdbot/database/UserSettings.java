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
@JsonSerialize(as = ImmutableUserSettings.class)
@JsonDeserialize(as = ImmutableUserSettings.class)
public interface UserSettings {

    @Criteria.Id
    @JsonProperty("_id")
    long userId();

    boolean showDiscordOnProfile();

    static UserSettings defaultSettings(long userId) {
        return ImmutableUserSettings.builder()
                .userId(userId)
                .showDiscordOnProfile(false)
                .build();
    }
}
