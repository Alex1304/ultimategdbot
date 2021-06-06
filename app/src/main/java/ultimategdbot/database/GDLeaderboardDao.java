package ultimategdbot.database;

import jdash.common.entity.GDUserStats;
import org.immutables.criteria.backend.Backend;
import reactor.core.publisher.Mono;

import java.time.Instant;

public final class GDLeaderboardDao {

    private final GDLeaderboardRepository repository;

    public GDLeaderboardDao(Backend backend) {
        this.repository = new GDLeaderboardRepository(backend);
    }

    public Mono<GDUserStats> saveStats(GDUserStats userStats) {
        return repository
                .upsert(ImmutableGDLeaderboard.builder()
                        .accountId(userStats.accountId())
                        .name(userStats.name())
                        .lastRefreshed(Instant.now())
                        .stars(userStats.stars())
                        .diamonds(userStats.diamonds())
                        .userCoins(userStats.userCoins())
                        .secretCoins(userStats.secretCoins())
                        .demons(userStats.demons())
                        .creatorPoints(userStats.creatorPoints())
                        .build())
                .thenReturn(userStats);
    }
}
