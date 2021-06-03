package ultimategdbot.config;

import botrino.api.annotation.ConfigEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jdash.client.request.GDRequests;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.Set;

@ConfigEntry("ultimategdbot")
@Value.Immutable
@JsonDeserialize(as = ImmutableUltimateGDBotConfig.class)
public interface UltimateGDBotConfig {

    @Value.Default
    @JsonProperty("pagination_max_entries")
    default int paginationMaxEntries() {
        return 20;
    }

    @Value.Default
    @JsonProperty("emoji_guild_ids")
    default Set<Long> emojiGuildIds() {
        return Set.of();
    }

    GD gd();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGD.class)
    interface GD {

        @Value.Default
        @JsonProperty("icon_cache_max_size")
        default int iconCacheMaxSize() {
            return 2500;
        }

        @Value.Default
        @JsonProperty("icon_channel_id")
        default long iconChannelId() {
            return 0;
        }

        Client client();

        @Value.Immutable
        @JsonDeserialize(as = ImmutableClient.class)
        interface Client {

            String username();

            String password();

            @Value.Default
            default String host() {
                return GDRequests.BASE_URL;
            }

            @Value.Default
            @JsonProperty("cache_ttl_seconds")
            default int cacheTtlSeconds() {
                return 900;
            }

            @Value.Default
            @JsonProperty("request_timeout_seconds")
            default int requestTimeoutSeconds() {
                return 0;
            }

            @JsonProperty("request_limiter")
            Optional<RequestLimiter> requestLimiter();

            @Value.Immutable
            @JsonDeserialize(as = ImmutableRequestLimiter.class)
            interface RequestLimiter {

                @Value.Default
                default int limit() {
                    return 10;
                }

                @Value.Default
                @JsonProperty("interval_seconds")
                default int intervalSeconds() {
                    return 60;
                }
            }
        }
    }
}
