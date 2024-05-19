package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.GdModCriteria.gdMod;

@RdiService
public final class GdModDao {

    private final GdModRepository repository;

    @RdiFactory
    public GdModDao(DatabaseService db) {
        this.repository = new GdModRepository(db.getBackend());
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
