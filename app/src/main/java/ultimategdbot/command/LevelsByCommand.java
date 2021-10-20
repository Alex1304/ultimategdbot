package ultimategdbot.command;

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
import org.reactivestreams.Publisher;
import ultimategdbot.Strings;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDLevelService;
import ultimategdbot.service.GDUserService;

import java.util.List;

@RdiService
@ChatInputCommand(name = "levels-by", description = "Browse levels from a specific player in Geometry Dash.")
public final class LevelsByCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;
    private final GDClient gdClient;
    private final GDUserService userService;

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public LevelsByCommand(GDCommandCooldown commandCooldown, GDLevelService levelService, GDClient gdClient,
                           GDUserService userService) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
        this.gdClient = gdClient;
        this.userService = userService;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return grammar.resolve(ctx.event())
                .flatMap(args -> userService.stringToUser(ctx, args.gdUsername))
                .flatMap(gdUser -> levelService.interactiveSearch(ctx,
                        ctx.translate(Strings.GD, "player_levels", gdUser.name()),
                        page -> gdClient.browseLevelsByUser(gdUser.playerId(), page)))
                .then();
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

        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "gd-username",
                description = "The GD username of the target."
        )
        String gdUsername;
    }
}
