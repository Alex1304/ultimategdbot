package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.BotAdminCriteria.botAdmin;

@RdiService
public final class BotAdminDao {

    private final DatabaseService db;

    @RdiFactory
    public BotAdminDao(DatabaseService db) {
        this.db = db;
    }

    public Flux<Long> getAllIds() {
        return db.botAdminRepository().findAll().fetch()
                .map(BotAdmin::id);
    }

    public Mono<Void> add(long id) {
        return db.botAdminRepository()
                .upsert(ImmutableBotAdmin.builder().id(id).build())
                .then();
    }

    public Mono<Void> remove(long id) {
        return db.botAdminRepository()
                .delete(botAdmin.id.is(id))
                .then();
    }

    public Mono<Boolean> exists(long id) {
        return db.botAdminRepository()
                .find(botAdmin.id.is(id))
                .exists();
    }
}
