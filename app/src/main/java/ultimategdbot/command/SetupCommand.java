package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.api.util.Markdown;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.Scope;
import botrino.command.annotation.Alias;
import botrino.command.doc.CommandDocumentation;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CommandCategory(CommandCategory.GENERAL)
@Alias({"setup", "settings", "configure", "config"})
@RdiService
public class SetupCommand implements Command {

    private final Set<Command> setupEntries = new HashSet<>();
    private final OutputPaginator outputPaginator;
    private final PrivilegeFactory privilegeFactory;

    @RdiFactory
    public SetupCommand(OutputPaginator outputPaginator, PrivilegeFactory privilegeFactory) {
        this.outputPaginator = outputPaginator;
        this.privilegeFactory = privilegeFactory;
    }

    public void addAllSetupEntries(Collection<? extends Command> setupEntries) {
        this.setupEntries.addAll(setupEntries);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return outputPaginator.paginate(ctx, listFormattedEntries(ctx),
                content -> ctx.translate(Strings.GENERAL, "setup_intro") + "\n\n" + content);
    }

    private List<String> listFormattedEntries(CommandContext ctx) {
        return setupEntries.stream()
                .map(command -> Markdown.code(ctx.getPrefixUsed() + "setup " +
                        String.join("|", command.aliases())) +": " + command.documentation(ctx).getDescription())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "setup_description"))
                .build();
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.guildAdmin();
    }

    @Override
    public Scope scope() {
        return Scope.GUILD_ONLY;
    }

    @Override
    public Set<Command> subcommands() {
        return setupEntries;
    }
}
