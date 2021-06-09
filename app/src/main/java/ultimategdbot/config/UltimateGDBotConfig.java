package ultimategdbot.config;

import botrino.api.annotation.ConfigEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jdash.client.request.GDRequests;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ConfigEntry("ultimategdbot")
@Value.Immutable
@JsonDeserialize(as = ImmutableUltimateGDBotConfig.class)
public interface UltimateGDBotConfig {

    @Value.Default
    @JsonProperty("pagination_max_entries")
    default int paginationMaxEntries() {
        return 30;
    }

    @Value.Default
    @JsonProperty("emoji_guild_ids")
    default Set<Long> emojiGuildIds() {
        return Set.of();
    }

    GD gd();

    @JsonProperty("command_cooldown")
    Optional<Limiter> commandCooldown();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGD.class)
    interface GD {

        @Value.Default
        @JsonProperty("icon_cache_max_size")
        default int iconCacheMaxSize() {
            return 2500;
        }

        @JsonProperty("icon_channel_id")
        Optional<Long> iconChannelId();

        Client client();

        Events events();

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
            Optional<Limiter> requestLimiter();
        }

        @Value.Immutable
        @JsonDeserialize(as = ImmutableEvents.class)
        interface Events {

            @Value.Default
            @JsonProperty("event_loop_interval_seconds")
            default int eventLoopIntervalSeconds() {
                return 60;
            }

            @Value.Default
            default boolean crosspost() {
                return true;
            }

            @Value.Default
            @JsonProperty("rates_channel_ids")
            default List<Long> ratesChannelIds() {
                return List.of();
            }

            @Value.Default
            @JsonProperty("demons_channel_ids")
            default List<Long> demonsChannelIds() {
                return List.of();
            }

            @JsonProperty("timely_channel_id")
            Optional<Long> timelyChannelId();

            @JsonProperty("mods_channel_id")
            Optional<Long> modsChannelId();

            @JsonProperty("public_random_messages")
            RandomMessages publicRandomMessages();

            @JsonProperty("dm_random_messages")
            RandomMessages dmRandomMessages();

            @Value.Immutable
            @JsonDeserialize(as = ImmutableRandomMessages.class)
            interface RandomMessages {

                @JsonProperty("rates")
                List<String> rates();

                @JsonProperty("unrates")
                List<String> unrates();

                @JsonProperty("daily")
                List<String> daily();

                @JsonProperty("weekly")
                List<String> weekly();

                @JsonProperty("mod")
                List<String> mod();

                @JsonProperty("elder_mod")
                List<String> elderMod();

                @JsonProperty("unmod")
                List<String> unmod();

                @JsonProperty("elder_unmod")
                List<String> elderUnmod();
            }
        }
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableLimiter.class)
    interface Limiter {

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
