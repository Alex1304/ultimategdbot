package ultimategdbot.service;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import reactor.core.publisher.Mono;
import ultimategdbot.database.BlacklistDao;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RdiService
public final class BlacklistService {

    private final BlacklistDao blacklistDao;
    private final Set<Long> blacklistCache;

    private BlacklistService(BlacklistDao blacklistDao, Set<Long> blacklistCache) {
        this.blacklistDao = blacklistDao;
        this.blacklistCache = blacklistCache;
    }

    @RdiFactory
    public static Mono<BlacklistService> create(BlacklistDao blacklistDao) {
        return blacklistDao.getAllIds()
                .collect(Collectors.toCollection(HashSet::new))
                .map(Collections::synchronizedSet)
                .map(blacklistCache -> new BlacklistService(blacklistDao, blacklistCache));
    }

    public boolean isBlacklisted(InteractionCreateEvent event) {
        var guildId = event.getInteraction().getGuildId().map(Snowflake::asLong).orElse(null);
        var authorId = event.getInteraction().getUser().getId().asLong();
        var channelId = event.getInteraction().getChannelId().asLong();
        return (guildId == null || !blacklistCache.contains(guildId))
                && !blacklistCache.contains(authorId)
                && !blacklistCache.contains(channelId);
    }

    public Mono<Void> addToBlacklist(long id) {
        return blacklistDao.addToBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.add(id)));
    }

    public Mono<Void> removeFromBlacklist(long id) {
        return blacklistDao.removeFromBlacklist(id)
                .then(Mono.fromRunnable(() -> blacklistCache.remove(id)));
    }

    public Set<Long> blacklist() {
        return Collections.unmodifiableSet(blacklistCache);
    }
}
