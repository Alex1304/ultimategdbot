package ultimategdbot.service;

import botrino.command.CommandEventProcessor;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import ultimategdbot.database.BlacklistDao;
import ultimategdbot.database.GuildConfig;
import ultimategdbot.database.GuildConfigDao;
import ultimategdbot.database.ImmutableGuildConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@RdiService
public class UltimateGDBotCommandEventProcessor implements CommandEventProcessor {

    private final GuildConfigDao guildConfigDao;
    private final BlacklistDao blacklistDao;
    private final Map<Long, GuildConfig> guildConfigCache;
    private final Set<Long> blacklistCache;

    private UltimateGDBotCommandEventProcessor(GuildConfigDao guildConfigDao,
                                               BlacklistDao blacklistDao,
                                               Map<Long, GuildConfig> guildConfigCache,
                                               Set<Long> blacklistCache) {
        this.guildConfigDao = guildConfigDao;
        this.blacklistDao = blacklistDao;
        this.guildConfigCache = guildConfigCache;
        this.blacklistCache = blacklistCache;
    }

    @RdiFactory
    public static Mono<UltimateGDBotCommandEventProcessor> create(GuildConfigDao guildConfigDao,
                                                                  BlacklistDao blacklistDao) {
        return Mono.zip(
                    guildConfigDao.getAll().collect(Collectors.toMap(GuildConfig::guildId, Function.identity())),
                    blacklistDao.getAll().collect(Collectors.toSet()))
                .map(TupleUtils.function((guildConfigCache, blacklistCache) ->
                        new UltimateGDBotCommandEventProcessor(guildConfigDao, blacklistDao,
                                new ConcurrentHashMap<>(guildConfigCache),
                                Collections.synchronizedSet(new HashSet<>(blacklistCache)))));
    }

    @Override
    public Mono<Boolean> filter(MessageCreateEvent event) {
        var guildId = event.getGuildId().map(Snowflake::asLong).orElse(null);
        var authorId = event.getMessage().getAuthor().map(User::getId).map(Snowflake::asLong).orElseThrow();
        var channelId = event.getMessage().getChannelId().asLong();
        return CommandEventProcessor.super.filter(event)
                .filter(Boolean::booleanValue)
                .map(__ -> (guildId == null || !blacklistCache.contains(guildId))
                        && !blacklistCache.contains(authorId)
                        && !blacklistCache.contains(channelId));
    }

    @Override
    public Mono<String> prefixForEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getGuildId().map(Snowflake::asLong))
                .flatMap(guildId -> Mono.justOrEmpty(guildConfigCache.get(guildId)))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.prefix()));
    }

    @Override
    public Mono<Locale> localeForEvent(MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getGuildId().map(Snowflake::asLong))
                .flatMap(guildId -> Mono.justOrEmpty(guildConfigCache.get(guildId)))
                .flatMap(guildConfig -> Mono.justOrEmpty(guildConfig.locale()))
                .map(Locale::forLanguageTag);
    }

    public Mono<Void> changePrefixForGuild(long guildId, @Nullable String prefix) {
        return guildConfigDao.setPrefix(guildId, prefix)
                .then(Mono.fromRunnable(() -> guildConfigCache.compute(guildId, (k, oldV) -> {
                    var newConfig = ImmutableGuildConfig.builder();
                    if (oldV != null) newConfig.from(oldV);
                    return newConfig.guildId(guildId).prefix(Optional.ofNullable(prefix)).build();
                })));
    }

    public Mono<Void> changeLocaleForGuild(long guildId, @Nullable Locale locale) {
        return guildConfigDao.setLocale(guildId, locale)
                .then(Mono.fromRunnable(() -> guildConfigCache.compute(guildId, (k, oldV) -> {
                    var newConfig = ImmutableGuildConfig.builder();
                    if (oldV != null) newConfig.from(oldV);
                    return newConfig.guildId(guildId)
                            .locale(Optional.ofNullable(locale).map(Locale::toLanguageTag))
                            .build();
                })));
    }

    public Mono<Void> addToBlacklist(long id) {
        return blacklistDao.addToBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.add(id)));
    }

    public Mono<Void> removeFromBlacklist(long id) {
        return blacklistDao.removeFromBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.remove(id)));
    }
}
