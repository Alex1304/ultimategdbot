package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import botrino.command.grammar.CommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.common.LevelBrowseMode;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.GDLevelService;

import static ultimategdbot.util.InteractionUtils.writeOnlyIfRefresh;

@CommandCategory(CommandCategory.GD)
@Alias("level")
@TopLevelCommand
@RdiService
public final class LevelCommand implements Command {

    private final GDLevelService levelService;
    private final GDClient gdClient;

    private final CommandGrammar<Args> grammar = CommandGrammar.builder()
            .nextArgument("query")
            .build(Args.class);

    @RdiFactory
    public LevelCommand(GDLevelService levelService, GDClient gdClient) {
        this.levelService = levelService;
        this.gdClient = gdClient;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        final var gdClient = writeOnlyIfRefresh(ctx, this.gdClient);
        return grammar.resolve(ctx).map(args -> args.query).flatMap(query -> {
            if (!query.matches("[a-zA-Z0-9 _-]+")) {
                return Mono.error(new CommandFailedException(ctx.translate(Strings.GD, "error_invalid_characters")));
            }
            return levelService.interactiveSearch(ctx, ctx.translate(Strings.GD, "search_results", query),
                    page -> gdClient.browseLevels(LevelBrowseMode.SEARCH, query, null, page));
        }).then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "level_description"))
                .setBody(tr.translate(Strings.HELP, "level_body"))
                .addFlag(FlagInformation.builder()
                        .setValueFormat("refresh")
                        .setDescription(tr.translate(Strings.HELP, "common_flag_refresh"))
                        .build())
                .build();
    }

    private static final class Args {
        String query;
    }
}
