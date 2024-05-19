package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.events.object.AwardedLevelAdd;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import java.time.Instant;

@RdiService
public final class GdAwardedLevelDao {

    private final GdAwardedLevelRepository repository;

    @RdiFactory
    public GdAwardedLevelDao(DatabaseService db) {
        this.repository = new GdAwardedLevelRepository(db.getBackend());
    }

    public Mono<WriteResult> saveEvent(AwardedLevelAdd event) {
        return repository.upsert(ImmutableGdAwardedLevel.builder()
                .levelId(event.addedLevel().id())
                .insertDate(Instant.now())
                .downloads(event.addedLevel().downloads())
                .likes(event.addedLevel().likes())
                .build());
    }
}
