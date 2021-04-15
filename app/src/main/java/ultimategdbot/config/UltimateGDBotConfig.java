package ultimategdbot.config;

import botrino.api.annotation.ConfigEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Set;

@ConfigEntry("ultimategdbot")
@Value.Immutable
@JsonDeserialize(as = ImmutableUltimateGDBotConfig.class)
public interface UltimateGDBotConfig {

    @JsonProperty("pagination_max_entries")
    int paginationMaxEntries();

    @JsonProperty("emoji_guild_ids")
    Set<Long> emojiGuildIds();
}
