package ultimategdbot.framework;

import botrino.interaction.InteractionEventProcessor;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import ultimategdbot.database.GuildConfig;
import ultimategdbot.database.ImmutableGuildConfig;
import ultimategdbot.service.DatabaseService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@RdiService
public class UGDBEventProcessor implements InteractionEventProcessor {

    private final DatabaseService db;
    private final Map<Long, GuildConfig> guildConfigCache;
    private final Set<Long> blacklistCache;

    private UGDBEventProcessor(DatabaseService db, Map<Long, GuildConfig> guildConfigCache,
                               Set<Long> blacklistCache) {
        this.db = db;
        this.guildConfigCache = guildConfigCache;
        this.blacklistCache = blacklistCache;
    }

    @RdiFactory
    public static Mono<UGDBEventProcessor> create(DatabaseService db) {
        return Mono.zip(
                    db.guildConfigDao().getAll().collect(Collectors.toMap(GuildConfig::guildId, Function.identity())),
                    db.blacklistDao().getAllIds().collect(Collectors.toSet()))
                .map(TupleUtils.function((guildConfigCache, blacklistCache) ->
                        new UGDBEventProcessor(db, new ConcurrentHashMap<>(guildConfigCache),
                                Collections.synchronizedSet(new HashSet<>(blacklistCache)))));
    }

    @Override
    public Mono<Boolean> filter(InteractionCreateEvent event) {
        var guildId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(null);
        var authorId = event.getInteraction().getUser().getId().asLong();
        var channelId = event.getInteraction().getChannelId().asLong();
        return Mono.just((guildId == null || !blacklistCache.contains(guildId))
                        && !blacklistCache.contains(authorId)
                        && !blacklistCache.contains(channelId));
    }

    @Override
    public Mono<Locale> computeLocale(InteractionCreateEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getGuildId().map(Snowflake::asLong))
                .flatMap(guildId -> Mono.justOrEmpty(guildConfigCache.get(guildId)))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.locale()))
                .map(Locale::forLanguageTag);
    }

    public Mono<Void> changeLocaleForGuild(long guildId, @Nullable Locale locale) {
        return db.guildConfigDao().setLocale(guildId, locale)
                .then(Mono.fromRunnable(() -> guildConfigCache.compute(guildId, (k, oldV) -> {
                    var newConfig = ImmutableGuildConfig.builder();
                    if (oldV != null) newConfig.from(oldV);
                    return newConfig.guildId(guildId)
                            .locale(Optional.ofNullable(locale).map(Locale::toLanguageTag))
                            .build();
                })));
    }

    public GuildConfig getCurrentGuildConfig(long guildId) {
        return guildConfigCache.getOrDefault(guildId, ImmutableGuildConfig.builder().guildId(guildId).build());
    }

    public Mono<Void> addToBlacklist(long id) {
        return db.blacklistDao().addToBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.add(id)));
    }

    public Mono<Void> removeFromBlacklist(long id) {
        return db.blacklistDao().removeFromBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.remove(id)));
    }

    public Set<Long> blacklist() {
        return Collections.unmodifiableSet(blacklistCache);
    }
}
