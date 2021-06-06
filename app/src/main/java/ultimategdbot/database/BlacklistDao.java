package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ultimategdbot.database.BlacklistCriteria.blacklist;

public final class BlacklistDao {

    private final BlacklistRepository repository;

    public BlacklistDao(Backend backend) {
        this.repository = new BlacklistRepository(backend);
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
