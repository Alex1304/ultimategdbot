package ultimategdbot.config;

import botrino.api.annotation.ConfigEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@ConfigEntry("mongodb")
@Value.Immutable
@JsonDeserialize(as = ImmutableMongoDBConfig.class)
public interface MongoDBConfig {

    @Value.Default
    @JsonProperty("database_name")
    default String databaseName() {
        return "ultimategdbot";
    }

    @Value.Default
    @JsonProperty("connection_string")
    default String connectionString() {
        return "mongodb://localhost:27017";
    }
}
