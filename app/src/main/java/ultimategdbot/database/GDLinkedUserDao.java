package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.backend.WriteResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static ultimategdbot.database.GDLinkedUserCriteria.gDLinkedUser;

public final class GDLinkedUserDao {

    private final GDLinkedUserRepository repository;

    public GDLinkedUserDao(Backend backend) {
        this.repository = new GDLinkedUserRepository(backend);
    }

    public Flux<Long> getDiscordAccountsForGDUser(long gdUserId) {
        return repository
                .find(gDLinkedUser.gdUserId.is(gdUserId).and(gDLinkedUser.isLinkActivated.isTrue()))
                .fetch()
                .map(GDLinkedUser::discordUserId);
    }

    public Mono<GDLinkedUser> get(long discordUserId) {
        return repository
                .find(gDLinkedUser.discordUserId.is(discordUserId))
                .oneOrNone();
    }

    public Mono<GDLinkedUser> getActiveLink(long discordUserId) {
        return repository
                .find(gDLinkedUser.discordUserId.is(discordUserId).and(gDLinkedUser.isLinkActivated.isTrue()))
                .oneOrNone();
    }

    public Mono<WriteResult> save(GDLinkedUser linkedUser) {
        return repository.upsert(linkedUser);
    }

    public Mono<WriteResult> confirmLink(long discordUserId) {
        return repository.update(gDLinkedUser.discordUserId.is(discordUserId))
                .set(gDLinkedUser.confirmationToken, null)
                .set(gDLinkedUser.isLinkActivated, true)
                .execute();
    }

    public Mono<WriteResult> delete(long discordUserId) {
        return repository.delete(gDLinkedUser.discordUserId.is(discordUserId));
    }
}
