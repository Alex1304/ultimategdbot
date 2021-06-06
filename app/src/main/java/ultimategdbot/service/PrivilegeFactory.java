package ultimategdbot.service;

import botrino.command.privilege.Privilege;
import botrino.command.privilege.Privileges;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import ultimategdbot.exception.BotAdminPrivilegeException;
import ultimategdbot.exception.BotOwnerPrivilegeException;
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
        return ctx -> Snowflake.asLong(ctx.event().getMessage().getUserData().id()) == ownerId
                ? Mono.empty()
                : Mono.error(new BotOwnerPrivilegeException());
    }

    public Privilege botAdmin() {
        return botOwner().or(ctx -> db.botAdminDao()
                .exists(Snowflake.asLong(ctx.event().getMessage().getUserData().id()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(BotAdminPrivilegeException::new))
                .then(), (a, b) -> b);
    }

    public Privilege guildAdmin() {
        return botAdmin().or(Privileges.checkPermissions(ctx -> new GuildAdminPrivilegeException(),
                perms -> perms.contains(Permission.ADMINISTRATOR)), (a, b) -> b);
    }
}
