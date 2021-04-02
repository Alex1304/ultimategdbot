package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.BlacklistCriteria.blacklist;

@RdiService
public final class BlacklistDao {

    private final DatabaseService db;

    @RdiFactory
    public BlacklistDao(DatabaseService db) {
        this.db = db;
    }

    public Flux<Long> getAll() {
        return db.blacklistRepository().findAll().fetch()
                .map(Blacklist::id);
    }

    public Mono<Void> addToBlacklist(long id) {
        return db.blacklistRepository()
                .upsert(ImmutableBlacklist.builder().id(id).build())
                .then();
    }

    public Mono<Void> removeFromBlacklist(long id) {
        return db.blacklistRepository()
                .delete(blacklist.id.is(id))
                .then();
    }
}
