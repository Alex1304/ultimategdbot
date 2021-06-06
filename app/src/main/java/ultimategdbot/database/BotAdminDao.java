package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ultimategdbot.database.BotAdminCriteria.botAdmin;

public final class BotAdminDao {

    private final BotAdminRepository repository;

    public BotAdminDao(Backend backend) {
        this.repository = new BotAdminRepository(backend);
    }

    public Flux<Long> getAllIds() {
        return repository.findAll().fetch().map(BotAdmin::id);
    }

    public Mono<Void> add(long id) {
        return repository.upsert(ImmutableBotAdmin.of(id)).then();
    }

    public Mono<Void> remove(long id) {
        return repository.delete(botAdmin.id.is(id)).then();
    }

    public Mono<Boolean> exists(long id) {
        return repository.find(botAdmin.id.is(id)).exists();
    }
}
