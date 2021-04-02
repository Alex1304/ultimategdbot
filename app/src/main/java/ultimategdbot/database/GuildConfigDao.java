package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import ultimategdbot.service.DatabaseService;

import java.util.Locale;
import java.util.Optional;

@RdiService
public final class GuildConfigDao {

    private final DatabaseService db;

    @RdiFactory
    public GuildConfigDao(DatabaseService db) {
        this.db = db;
    }

    public Mono<Void> setPrefix(long guildId, @Nullable String newPrefix) {
        var repository = db.guildConfigRepository();
        return repository
                .upsert(ImmutableGuildConfig.builder()
                        .guildId(guildId)
                        .prefix(Optional.ofNullable(newPrefix))
                        .build())
                .then();
    }

    public Mono<Void> setLocale(long guildId, @Nullable Locale newLocale) {
        var repository = db.guildConfigRepository();
        return repository
                .upsert(ImmutableGuildConfig.builder()
                        .guildId(guildId)
                        .locale(Optional.ofNullable(newLocale).map(Locale::toLanguageTag))
                        .build())
                .then();
    }

    public Flux<GuildConfig> getAll() {
        return db.guildConfigRepository().findAll().fetch();
    }
}
