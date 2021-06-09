package ultimategdbot.database;

import jdash.events.object.AwardedAdd;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Mono;

import java.time.Instant;

public final class GdAwardedLevelDao {

    private final GdAwardedLevelRepository repository;

    public GdAwardedLevelDao(Backend backend) {
        this.repository = new GdAwardedLevelRepository(backend);
    }

    public Mono<WriteResult> saveEvent(AwardedAdd event) {
        return repository.upsert(ImmutableGdAwardedLevel.builder()
                .levelId(event.addedLevel().id())
                .insertDate(Instant.now())
                .downloads(event.addedLevel().downloads())
                .likes(event.addedLevel().likes())
                .build());
    }
}
