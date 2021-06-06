package ultimategdbot.database;

import org.immutables.criteria.backend.Backend;
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

    public Mono<GDLinkedUser> getByDiscordUserId(long discordUserId) {
        return repository
                .find(gDLinkedUser.discordUserId.is(discordUserId).and(gDLinkedUser.isLinkActivated.isTrue()))
                .oneOrNone();
    }
}
