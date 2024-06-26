package ultimategdbot.database;

import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.service.DatabaseService;

import java.util.List;

import static ultimategdbot.database.GdLinkedUserCriteria.gdLinkedUser;

@RdiService
public final class GdLinkedUserDao {

    private final GdLinkedUserRepository repository;

    @RdiFactory
    public GdLinkedUserDao(DatabaseService db) {
        this.repository = new GdLinkedUserRepository(db.getBackend());
    }

    public Flux<Long> getDiscordAccountsForGDUser(long gdUserId) {
        return repository
                .find(gdLinkedUser.gdUserId.is(gdUserId).and(gdLinkedUser.isLinkActivated.isTrue()))
                .fetch()
                .map(GdLinkedUser::discordUserId);
    }

    public Mono<GdLinkedUser> get(long discordUserId) {
        return repository.find(gdLinkedUser.discordUserId.is(discordUserId)).oneOrNone();
    }

    public Mono<GdLinkedUser> getActiveLink(long discordUserId) {
        return repository
                .find(gdLinkedUser.discordUserId.is(discordUserId).and(gdLinkedUser.isLinkActivated.isTrue()))
                .oneOrNone();
    }

    public Flux<GdLinkedUser> getAllActiveLinks(long discordUserId) {
        return getActiveLink(discordUserId)
                .flatMapMany(linkedUser -> repository
                        .find(gdLinkedUser.gdUserId.is(linkedUser.gdUserId())
                                .and(gdLinkedUser.isLinkActivated.isTrue()))
                        .limit(25)
                        .fetch());
    }

    public Flux<GdLinkedUser> getAllIn(List<Long> discordUserIds) {
        return repository.find(gdLinkedUser.discordUserId.in(discordUserIds)).fetch();
    }

    public Mono<WriteResult> save(GdLinkedUser linkedUser) {
        return repository.upsert(linkedUser);
    }

    public Mono<WriteResult> confirmLink(long discordUserId) {
        return repository.update(gdLinkedUser.discordUserId.is(discordUserId))
                .set(gdLinkedUser.confirmationToken, null)
                .set(gdLinkedUser.isLinkActivated, true)
                .execute();
    }

    public Mono<WriteResult> delete(long discordUserId) {
        return repository.delete(gdLinkedUser.discordUserId.is(discordUserId));
    }
}
