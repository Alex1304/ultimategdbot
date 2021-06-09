package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static ultimategdbot.database.GdLeaderboardBanCriteria.gdLeaderboardBan;

public final class GdLeaderboardBanDao {

    private final GdLeaderboardBanRepository repository;

    public GdLeaderboardBanDao(Backend backend) {
        this.repository = new GdLeaderboardBanRepository(backend);
    }

    public Flux<GdLeaderboardBan> getAll() {
        return repository.findAll().fetch();
    }

    public Flux<GdLeaderboardBan> getAllIn(List<Long> gdUserIds) {
        return repository.find(gdLeaderboardBan.accountId.in(gdUserIds)).fetch();
    }

    public Mono<WriteResult> save(GdLeaderboardBan ban) {
        return repository.upsert(ban);
    }

    public Mono<WriteResult> delete(long accountId) {
        return repository.delete(gdLeaderboardBan.accountId.is(accountId));
    }
}
