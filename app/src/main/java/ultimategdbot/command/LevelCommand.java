package ultimategdbot.command;

import botrino.interaction.InteractionFailedException;
import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.grammar.ChatInputCommandGrammar;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import jdash.client.GDClient;
import jdash.common.LevelSearchMode;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDLevelService;

import java.util.List;

@RdiService
@ChatInputCommand(name = "level", description = "Search for online levels in Geometry Dash.")
public final class LevelCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;
    private final GDClient gdClient;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public LevelCommand(GDCommandCooldown commandCooldown, GDLevelService levelService, GDClient gdClient) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
        this.gdClient = gdClient;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event()).map(args -> args.query).flatMap(query -> {
            if (!query.matches("[a-zA-Z0-9 _-]+")) {
                return Mono.error(new InteractionFailedException(ctx.translate(Strings.GD, "error_invalid_characters")));
            }
            return levelService.interactiveSearch(ctx, ctx.translate(Strings.GD, "search_results", query),
                    page -> gdClient.searchLevels(LevelSearchMode.SEARCH, query, null, page));
        }).then();
    }

    @Override
    public List<ApplicationCommandOptionData> options() {
        return grammar.toOptions();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private static final class Options {

        @SuppressWarnings("NotNullFieldNotInitialized")
        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "query",
                description = "The search query.",
                required = true
        )
        String query;
    }
}
