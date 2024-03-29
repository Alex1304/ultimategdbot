package ultimategdbot.service;

import botrino.interaction.privilege.Privilege;
import botrino.interaction.privilege.Privileges;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import ultimategdbot.database.GdMod;
import ultimategdbot.exception.BotAdminPrivilegeException;
import ultimategdbot.exception.BotOwnerPrivilegeException;
import ultimategdbot.exception.ElderModPrivilegeException;
import ultimategdbot.exception.GuildAdminPrivilegeException;

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

    public Privilege botAdmin() {
        return botOwner().or(ctx -> db.botAdminDao()
                .exists(ctx.event().getInteraction().getUser().getId().asLong())
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(BotAdminPrivilegeException::new))
                .then(), (a, b) -> b);
    }

    public Privilege guildAdmin() {
        return botAdmin().or(Privileges.checkPermissions(ctx -> new GuildAdminPrivilegeException(),
                perms -> perms.contains(Permission.ADMINISTRATOR)), (a, b) -> b);
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
