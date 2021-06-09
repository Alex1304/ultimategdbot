package ultimategdbot.command;

import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.PrivilegeFactory;

@CommandCategory(CommandCategory.GENERAL)
@Alias("logout")
@TopLevelCommand
@RdiService
public final class LogoutCommand implements Command {

    private final PrivilegeFactory privilegeFactory;

    @RdiFactory
    public LogoutCommand(PrivilegeFactory privilegeFactory) {
        this.privilegeFactory = privilegeFactory;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return ctx.channel().createMessage(ctx.translate(Strings.GENERAL, "disconnecting"))
                .then(ctx.event().getClient().logout());
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.botOwner();
    }
}
