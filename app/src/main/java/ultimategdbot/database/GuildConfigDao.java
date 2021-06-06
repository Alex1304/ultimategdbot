package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.Locale;
import java.util.Optional;

public final class GuildConfigDao {

    private final GuildConfigRepository repository;

    public GuildConfigDao(Backend backend) {
        this.repository = new GuildConfigRepository(backend);
    }

    public Mono<Void> setPrefix(long guildId, @Nullable String newPrefix) {
        return repository
                .upsert(ImmutableGuildConfig.builder()
                        .guildId(guildId)
                        .prefix(Optional.ofNullable(newPrefix))
                        .build())
                .then();
    }

    public Mono<Void> setLocale(long guildId, @Nullable Locale newLocale) {
        return repository
                .upsert(ImmutableGuildConfig.builder()
                        .guildId(guildId)
                        .locale(Optional.ofNullable(newLocale).map(Locale::toLanguageTag))
                        .build())
                .then();
    }

    public Flux<GuildConfig> getAll() {
        return repository.findAll().fetch();
    }
}
