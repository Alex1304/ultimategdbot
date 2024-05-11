package ultimategdbot.service;

import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.database.GdMod;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.ElderModPrivilegeException;

@RdiService
public final class PrivilegeFactory {

    private final long ownerId;
    private final DatabaseService db;

    @RdiFactory
    public PrivilegeFactory(ApplicationInfo applicationInfo, DatabaseService db) {
        this.ownerId = applicationInfo.getOwnerId().asLong();
        this.db = db;
    }

    public Privilege botOwner() {
        return ctx -> ctx.event().getInteraction().getUser().getId().asLong() == ownerId
                ? Mono.empty()
                : Mono.error(new BotOwnerPrivilegeException());
    }

    public Privilege elderMod() {
        return botOwner().or(ctx -> db.gdLinkedUserDao()
                .getActiveLink(ctx.event().getInteraction().getUser().getId().asLong())
                .flatMap(linkedUser -> db.gdModDao().get(linkedUser.gdUserId()).map(GdMod::isElder))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(ElderModPrivilegeException::new))
                .then(), (a, b) -> b);
    }
}
