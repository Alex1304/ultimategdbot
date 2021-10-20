package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.interaction.cooldown.Cooldown;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import ultimategdbot.config.UltimateGDBotConfig;

import java.time.Duration;

@RdiService
public final class GDCommandCooldown {

    private final Cooldown cooldown;

    @RdiFactory
    public GDCommandCooldown(ConfigContainer configContainer) {
        final var config = configContainer.get(UltimateGDBotConfig.class);
        this.cooldown = config.commandCooldown()
                .map(c -> Cooldown.of(c.limit(), Duration.ofSeconds(c.intervalSeconds())))
                .orElse(Cooldown.none());
    }

    public Cooldown get() {
        return cooldown;
    }
}
