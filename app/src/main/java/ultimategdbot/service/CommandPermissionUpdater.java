package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.interaction.config.InteractionConfig;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.ApplicationCommandPermissionsRequest;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.config.UltimateGDBotConfig;

import java.time.Duration;
import java.util.List;

@RdiService
public final class CommandPermissionUpdater {

    private static final Logger LOGGER = Loggers.getLogger(CommandPermissionUpdater.class);

    private final long applicationId;
    private final List<UltimateGDBotConfig.CommandPermission> permissions;
    private final GatewayDiscordClient gateway;
    private final Long guildId;

    @RdiFactory
    public CommandPermissionUpdater(ConfigContainer configContainer, ApplicationInfo applicationInfo,
                                    GatewayDiscordClient gateway) {
        this.applicationId = applicationInfo.getId().asLong();
        this.permissions = configContainer.get(UltimateGDBotConfig.class).commandPermissions();
        this.gateway = gateway;
        this.guildId = configContainer.get(InteractionConfig.class).applicationCommandsGuildId().orElse(null);
    }

    public Mono<Void> run() {
        final var appService = gateway.rest().getApplicationService();
        return Mono.delay(Duration.ofSeconds(10)) // Ensure this is run after commands are deployed
                .thenMany(guildId == null ? appService.getGlobalApplicationCommands(applicationId) :
                        appService.getGuildApplicationCommands(applicationId, guildId))
                .flatMap(command -> permissions.stream()
                        .filter(perm -> perm.name().equals(command.name()))
                        .map(perm -> appService.modifyApplicationCommandPermissions(applicationId, perm.guildId(),
                                Snowflake.asLong(command.id()), ApplicationCommandPermissionsRequest.builder()
                                                .addPermission(ApplicationCommandPermissionsData.builder()
                                                        .id(perm.roleId())
                                                        .type(1)
                                                        .permission(true)
                                                        .build())
                                        .build())
                                .doOnNext(__ -> LOGGER.debug("Updated permission for command {}", perm.name())))
                        .findAny()
                        .orElse(Mono.empty()))
                .then();
    }
}
