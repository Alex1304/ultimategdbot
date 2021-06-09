package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ultimategdbot.database.GdModCriteria.gdMod;

public final class GdModDao {

    private final GdModRepository repository;

    public GdModDao(Backend backend) {
        this.repository = new GdModRepository(backend);
    }

    public Mono<GdMod> get(long accountId) {
        return repository.find(gdMod.accountId.is(accountId)).oneOrNone();
    }

    public Flux<GdMod> getAll() {
        return repository.findAll().fetch();
    }

    public Mono<WriteResult> save(GdMod mod) {
        return repository.upsert(mod);
    }

    public Mono<WriteResult> delete(long accountId) {
        return repository.delete(gdMod.accountId.is(accountId));
    }
}
