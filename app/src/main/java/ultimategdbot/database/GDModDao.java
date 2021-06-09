package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ultimategdbot.database.GDModCriteria.gDMod;

public final class GDModDao {

    private final GDModRepository repository;

    public GDModDao(Backend backend) {
        this.repository = new GDModRepository(backend);
    }

    public Mono<GDMod> get(long accountId) {
        return repository.find(gDMod.accountId.is(accountId)).oneOrNone();
    }

    public Flux<GDMod> getAll() {
        return repository.findAll().fetch();
    }

    public Mono<WriteResult> save(GDMod mod) {
        return repository.upsert(mod);
    }

    public Mono<WriteResult> delete(long accountId) {
        return repository.delete(gDMod.accountId.is(accountId));
    }
}
