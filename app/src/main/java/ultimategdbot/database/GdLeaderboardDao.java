package ultimategdbot.database;

import jdash.common.entity.GDUserStats;
import org.immutables.criteria.backend.Backend;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static ultimategdbot.database.GdLeaderboardCriteria.gdLeaderboard;

public final class GdLeaderboardDao {

    private final GdLeaderboardRepository repository;

    public GdLeaderboardDao(Backend backend) {
        this.repository = new GdLeaderboardRepository(backend);
    }

    public Mono<GDUserStats> saveStats(GDUserStats userStats) {
        return repository
                .upsert(ImmutableGdLeaderboard.builder()
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

    public Flux<GdLeaderboard> getAllIn(List<Long> gdUserIds) {
        return repository.find(gdLeaderboard.accountId.in(gdUserIds)).fetch();
    }
}
