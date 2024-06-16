package ultimategdbot.service;

import botrino.interaction.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.database.GdLinkedUserDao;
import ultimategdbot.database.GdMod;
import ultimategdbot.database.GdModDao;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.ElderModPrivilegeException;

@RdiService
public final class PrivilegeFactory {

    private final long ownerId;
    private final GdLinkedUserDao gdLinkedUserDao;
    private final GdModDao gdModDao;

    @RdiFactory
    public PrivilegeFactory(ApplicationInfo applicationInfo, GdLinkedUserDao gdLinkedUserDao, GdModDao gdModDao) {
        this.ownerId = applicationInfo.getOwnerId().asLong();
        this.gdLinkedUserDao = gdLinkedUserDao;
        this.gdModDao = gdModDao;
    }

    public Privilege botOwner() {
        return ctx -> ctx.event().getInteraction().getUser().getId().asLong() == ownerId
                ? Mono.empty()
                : Mono.error(new BotOwnerPrivilegeException());
    }

    public Privilege elderMod() {
        return botOwner().or(ctx -> gdLinkedUserDao
                .getActiveLink(ctx.event().getInteraction().getUser().getId().asLong())
                .flatMap(linkedUser -> gdModDao.get(linkedUser.gdUserId()).map(GdMod::isElder))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(ElderModPrivilegeException::new))
                .then(), (a, b) -> b);
    }
}
