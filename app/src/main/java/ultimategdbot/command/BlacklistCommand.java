package ultimategdbot.command;

import botrino.command.Command;
import botrino.command.ParentCommand;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.grammar.CommandGrammar;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import ultimategdbot.Strings;
import ultimategdbot.service.PrivilegeFactory;
import ultimategdbot.service.UltimateGDBotCommandEventProcessor;

import java.util.Set;

@Alias("blacklist")
@TopLevelCommand
@RdiService
public final class BlacklistCommand implements ParentCommand {

    private final PrivilegeFactory privilegeFactory;
    private final UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor;

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("id", ArgumentMapper.asLong())
            .build(Args.class);

    @RdiFactory
    public BlacklistCommand(PrivilegeFactory privilegeFactory,
                            UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor) {
        this.privilegeFactory = privilegeFactory;
        this.ultimateGDBotCommandEventProcessor = ultimateGDBotCommandEventProcessor;
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.botOwner();
    }

    @Override
    public Set<Command> subcommands() {
        return Set.of(
                Command.builder("add", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> ultimateGDBotCommandEventProcessor.addToBlacklist(args.id)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "blacklist_success", args.id))
                                        .then())))
                        .inheritFrom(this)
                        .build(),
                Command.builder("remove", ctx -> grammar.resolve(ctx)
                        .flatMap(args -> ultimateGDBotCommandEventProcessor.removeFromBlacklist(args.id)
                                .then(ctx.channel()
                                        .createMessage(ctx.translate(Strings.APP, "unblacklist_success", args.id))
                                        .then())))
                        .inheritFrom(this)
                        .build());
    }

    private final static class Args {
        private long id;
    }
}
