package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static ultimategdbot.database.GDLeaderboardBanCriteria.gDLeaderboardBan;

public final class GDLeaderboardBanDao {

    private final GDLeaderboardBanRepository repository;

    public GDLeaderboardBanDao(Backend backend) {
        this.repository = new GDLeaderboardBanRepository(backend);
    }

    public Flux<GDLeaderboardBan> getAll() {
        return repository.findAll().fetch();
    }

    public Flux<GDLeaderboardBan> getAllIn(List<Long> gdUserIds) {
        return repository.find(gDLeaderboardBan.accountId.in(gdUserIds)).fetch();
    }

    public Mono<WriteResult> save(GDLeaderboardBan ban) {
        return repository.upsert(ban);
    }

    public Mono<WriteResult> delete(long accountId) {
        return repository.delete(gDLeaderboardBan.accountId.is(accountId));
    }
}
