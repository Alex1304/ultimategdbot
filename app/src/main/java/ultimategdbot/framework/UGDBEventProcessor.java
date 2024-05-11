package ultimategdbot.framework;

import botrino.interaction.InteractionEventProcessor;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RdiService
public class UGDBEventProcessor implements InteractionEventProcessor {

    private final DatabaseService db;
    private final Set<Long> blacklistCache;

    private UGDBEventProcessor(DatabaseService db,
                               Set<Long> blacklistCache) {
        this.db = db;
        this.blacklistCache = blacklistCache;
    }

    @RdiFactory
    public static Mono<UGDBEventProcessor> create(DatabaseService db) {
        return db.blacklistDao().getAllIds().collect(Collectors.toSet())
                .map((blacklistCache) -> new UGDBEventProcessor(db,
                        Collections.synchronizedSet(new HashSet<>(blacklistCache))));
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
