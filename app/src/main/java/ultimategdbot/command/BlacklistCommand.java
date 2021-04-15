package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.grammar.ArgumentMapper;
import botrino.command.privilege.Privilege;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.framework.UltimateGDBotCommandEventProcessor;
import ultimategdbot.service.OutputPaginator;
import ultimategdbot.service.PrivilegeFactory;

@Alias("blacklist")
@TopLevelCommand
@RdiService
public final class BlacklistCommand extends AddRemoveListCommand<Long> {

    private final PrivilegeFactory privilegeFactory;
    private final UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor;

    @RdiFactory
    public BlacklistCommand(OutputPaginator outputPaginator,
                            PrivilegeFactory privilegeFactory,
                            UltimateGDBotCommandEventProcessor ultimateGDBotCommandEventProcessor) {
        super(outputPaginator);
        this.privilegeFactory = privilegeFactory;
        this.ultimateGDBotCommandEventProcessor = ultimateGDBotCommandEventProcessor;
    }

    @Override
    public Privilege privilege() {
        return privilegeFactory.botOwner();
    }

    @Override
    ArgumentMapper<Long> argumentMapper() {
        return ArgumentMapper.asLong();
    }

    @Override
    Mono<Void> add(CommandContext ctx, Long id) {
        return ultimateGDBotCommandEventProcessor.addToBlacklist(id);
    }

    @Override
    Mono<Void> remove(CommandContext ctx, Long id) {
        return ultimateGDBotCommandEventProcessor.removeFromBlacklist(id);
    }

    @Override
    Flux<String> listFormattedItems(CommandContext ctx) {
        return Flux.fromIterable(ultimateGDBotCommandEventProcessor.blacklist())
                .sort()
                .map(String::valueOf);
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.APP, "description_blacklist"))
                .build();
    }
}
