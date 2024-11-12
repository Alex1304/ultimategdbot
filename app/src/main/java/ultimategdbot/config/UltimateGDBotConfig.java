package ultimategdbot.config;

import botrino.api.annotation.ConfigEntry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@ConfigEntry("ultimategdbot")
@Value.Immutable
@JsonDeserialize(as = ImmutableUltimateGDBotConfig.class)
public interface UltimateGDBotConfig {

    @JsonProperty("pagination_max_entries")
    int paginationMaxEntries();

    @JsonProperty("emoji_guild_ids")
    Set<Long> emojiGuildIds();

    @JsonProperty("command_cooldown")
    Optional<Limiter> commandCooldown();

    @JsonProperty("legacy_command_redirect_prefix")
    Optional<String> legacyCommandRedirectPrefix();

    @Value.Default
    @JsonProperty("command_permissions")
    default List<CommandPermission> commandPermissions() {
        return List.of();
    }

    @Value.Default
    @JsonProperty("bot_announcements_channel_ids")
    default Set<Long> botAnnouncementsChannelIds() {
        return Set.of();
    }

    @Value.Immutable
    @JsonDeserialize(as = ImmutableCommandPermission.class)
    interface CommandPermission {

        String name();

        @JsonProperty("guild_id")
        long guildId();

        @JsonProperty("role_id")
        long roleId();
    }

    GD gd();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableGD.class)
    interface GD {

        Client client();

        Events events();

        @Value.Immutable
        @JsonDeserialize(as = ImmutableClient.class)
        interface Client {

            String username();

            String password();

            String host();

            @JsonProperty("cache_ttl_seconds")
            int cacheTtlSeconds();

            @JsonProperty("request_timeout_seconds")
            int requestTimeoutSeconds();

            @JsonProperty("request_limiter")
            Optional<Limiter> requestLimiter();
        }

        @Value.Immutable
        @JsonDeserialize(as = ImmutableEvents.class)
        interface Events {

            @JsonProperty("event_loop_interval_seconds")
            int eventLoopIntervalSeconds();

            boolean crosspost();

            @JsonProperty("rates_channel_ids")
            Set<Long> ratesChannelIds();

            @JsonProperty("demons_channel_ids")
            Set<Long> demonsChannelIds();

            @JsonProperty("timely_channel_id")
            Optional<Long> timelyChannelId();

            @JsonProperty("mods_channel_id")
            Optional<Long> modsChannelId();

            @JsonProperty("public_random_messages")
            RandomMessages publicRandomMessages();

            @JsonProperty("dm_random_messages")
            Optional<RandomMessages> dmRandomMessages();

            @Value.Immutable
            @JsonDeserialize(as = ImmutableRandomMessages.class)
            interface RandomMessages {

                List<String> rates();

                List<String> unrates();

                List<String> daily();

                List<String> weekly();

                List<String> event();

                List<String> mod();

                @JsonProperty("elder_mod")
                List<String> elderMod();

                @JsonProperty("lb_mod")
                List<String> lbMod();

                List<String> unmod();

                @JsonProperty("elder_unmod")
                List<String> elderUnmod();

                @JsonProperty("lb_unmod")
                List<String> lbUnmod();
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
