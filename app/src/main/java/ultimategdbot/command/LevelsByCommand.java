package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.cooldown.Cooldown;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import botrino.command.grammar.CommandGrammar;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import jdash.client.GDClient;
import jdash.common.entity.GDUserProfile;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDLevelService;
import ultimategdbot.service.GDUserService;

import static ultimategdbot.util.InteractionUtils.writeOnlyIfRefresh;

@CommandCategory(CommandCategory.GD)
@Alias("levelsby")
@TopLevelCommand
@RdiService
public final class LevelsByCommand implements Command {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;
    private final GDClient gdClient;

    private final CommandGrammar<Args> grammar;

    @RdiFactory
    public LevelsByCommand(GDCommandCooldown commandCooldown, GDLevelService levelService, GDClient gdClient,
                           GDUserService userService) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
        this.gdClient = gdClient;
        this.grammar = CommandGrammar.builder()
                .nextArgument("gdUser", userService::stringToUser)
                .build(Args.class);
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        final var gdClient = writeOnlyIfRefresh(ctx, this.gdClient);
        return grammar.resolve(ctx)
                .map(args -> args.gdUser)
                .flatMap(gdUser -> levelService.interactiveSearch(ctx,
                        ctx.translate(Strings.GD, "player_levels", gdUser.name()),
                        page -> gdClient.browseLevelsByUser(gdUser.playerId(), page)))
                .then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setSyntax(grammar.toString())
                .setDescription(tr.translate(Strings.HELP, "levelsby_description"))
                .setBody(tr.translate(Strings.HELP, "levelsby_body"))
                .addFlag(FlagInformation.builder()
                        .setValueFormat("refresh")
                        .setDescription(tr.translate(Strings.HELP, "common_flag_refresh"))
                        .build())
                .build();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }

    private static final class Args {
        GDUserProfile gdUser;
    }
}
