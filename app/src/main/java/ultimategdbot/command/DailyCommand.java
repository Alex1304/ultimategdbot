package ultimategdbot.command;

import botrino.api.i18n.Translator;
import botrino.command.Command;
import botrino.command.CommandContext;
import botrino.command.annotation.Alias;
import botrino.command.annotation.TopLevelCommand;
import botrino.command.cooldown.Cooldown;
import botrino.command.doc.CommandDocumentation;
import botrino.command.doc.FlagInformation;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDLevelService;

@CommandCategory(CommandCategory.GD)
@Alias({"daily", "dailylevel"})
@TopLevelCommand
@RdiService
public final class DailyCommand implements Command {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;

    @RdiFactory
    public DailyCommand(GDCommandCooldown commandCooldown, GDLevelService levelService) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
    }

    @Override
    public Mono<Void> run(CommandContext ctx) {
        return levelService.sendTimelyInfo(ctx, false).then();
    }

    @Override
    public CommandDocumentation documentation(Translator tr) {
        return CommandDocumentation.builder()
                .setDescription(tr.translate(Strings.HELP, "daily_description"))
                .setBody(tr.translate(Strings.HELP, "daily_body"))
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
}
