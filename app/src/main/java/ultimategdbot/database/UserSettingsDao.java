package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import static ultimategdbot.database.UserSettingsCriteria.userSettings;

@RdiService
public final class UserSettingsDao {

    private final UserSettingsRepository repository;

    @RdiFactory
    public UserSettingsDao(DatabaseService db) {
        this.repository = new UserSettingsRepository(db.getBackend());
    }

    public Mono<UserSettings> getById(long id) {
        return repository.find(userSettings.userId.is(id))
                .oneOrNone()
                .switchIfEmpty(Mono.just(ImmutableUserSettings.of(id)));
    }

    public Mono<WriteResult> save(UserSettings userSettings) {
        return repository.upsert(userSettings);
    }
}
