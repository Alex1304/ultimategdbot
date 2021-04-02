package ultimategdbot.service;

import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.exception.BotOwnerPrivilegeException;

@RdiService
public final class PrivilegeFactory {

    private final long ownerId;

    @RdiFactory
    public PrivilegeFactory(ApplicationInfo applicationInfo) {
        this.ownerId = applicationInfo.getOwnerId().asLong();
    }

    public Privilege botOwner() {
        return ctx -> Snowflake.asLong(ctx.event().getMessage().getUserData().id()) == ownerId
                ? Mono.empty()
                : Mono.error(new BotOwnerPrivilegeException());
    }
}
