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
@ChatInputCommand(name = "event", description = "Displays info on the current Event level.")
public final class EventCommand implements ChatInputInteractionListener {

    private final GDCommandCooldown commandCooldown;
    private final GDLevelService levelService;

    @RdiFactory
    public EventCommand(GDCommandCooldown commandCooldown, GDLevelService levelService) {
        this.commandCooldown = commandCooldown;
        this.levelService = levelService;
    }

    @Override
    public Publisher<?> run(ChatInputInteractionContext ctx) {
        return levelService.sendTimelyInfo(ctx, EmbedType.EVENT_LEVEL).then();
    }

    @Override
    public Cooldown cooldown() {
        return commandCooldown.get();
    }
}
