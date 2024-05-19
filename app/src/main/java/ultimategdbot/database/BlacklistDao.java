package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.BlacklistCriteria.blacklist;

@RdiService
public final class BlacklistDao {

    private final BlacklistRepository repository;

    @RdiFactory
    public BlacklistDao(DatabaseService db) {
        this.repository = new BlacklistRepository(db.getBackend());
    }

    public Flux<Long> getAllIds() {
        return repository.findAll().fetch()
                .map(Blacklist::id);
    }

    public Mono<Void> addToBlacklist(long id) {
        return repository.upsert(ImmutableBlacklist.of(id)).then();
    }

    public Mono<Void> removeFromBlacklist(long id) {
        return repository
                .delete(blacklist.id.is(id))
                .then();
    }
}
