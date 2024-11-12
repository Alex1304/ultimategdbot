package ultimategdbot.command;

import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.context.ChatInputInteractionContext;
import botrino.interaction.cooldown.Cooldown;
import botrino.interaction.listener.ChatInputInteractionListener;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import org.reactivestreams.Publisher;
import ultimategdbot.service.GDCommandCooldown;
import ultimategdbot.service.GDLevelService;
import ultimategdbot.util.EmbedType;

@RdiService
@ChatInputCommand(name = "weekly", description = "Displays info on the current Weekly demon.")
public final class WeeklyCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;

    @RdiFactory
    public WeeklyCommand(GDCommandCooldown commandCooldown, GDLevelService levelService) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return levelService.sendTimelyInfo(ctx, EmbedType.WEEKLY_DEMON).then();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }
}