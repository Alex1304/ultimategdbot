package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.interaction.InteractionService;
import botrino.interaction.config.InteractionConfig;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandPermissionsData;
import discord4j.discordjson.json.PartialGuildApplicationCommandPermissionsData;
import discord4j.rest.service.ApplicationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.config.UltimateGDBotConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;

@RdiService
public final class CommandPermissionUpdater {

    private final long applicationId;
    private final List<UltimateGDBotConfig.CommandPermission> permissions;
    private final ApplicationService appService;
    private final Long guildId;
    private final Mono<Void> onCommandsDeployed;

    @RdiFactory
    public CommandPermissionUpdater(ConfigContainer configContainer, ApplicationInfo applicationInfo,
                                    GatewayDiscordClient gateway, InteractionService interactionService) {
        this.applicationId = applicationInfo.getId().asLong();
        this.permissions = configContainer.get(UltimateGDBotConfig.class).commandPermissions();
        this.appService = gateway.rest().getApplicationService();
        this.guildId = configContainer.get(InteractionConfig.class).applicationCommandsGuildId().orElse(null);
        this.onCommandsDeployed = interactionService.onCommandsDeployed();
    }

    public Mono<Void> run() {
        return onCommandsDeployed
                .thenMany(guildId == null ?
                        appService.getGlobalApplicationCommands(applicationId) :
                        appService.getGuildApplicationCommands(applicationId, guildId))
                .collectMap(ApplicationCommandData::name, ApplicationCommandData::id)
                .flatMapMany(commandIdsByName -> Flux.fromStream(permissions.stream()
                        .collect(groupingBy(UltimateGDBotConfig.CommandPermission::guildId))
                        .entrySet()
                        .stream()
                        .map(entry -> updatePermissions(commandIdsByName, entry.getKey(), entry.getValue()))))
                .flatMap(Function.identity())
                .then();
    }

    private Mono<Void> updatePermissions(Map<String, String> commandIdsByName, long guildId,
                                         List<UltimateGDBotConfig.CommandPermission> permissions) {
        final var payload = permissions.stream()
                .collect(groupingBy(UltimateGDBotConfig.CommandPermission::name))
                .entrySet()
                .stream()
                .map(byCommandName -> (PartialGuildApplicationCommandPermissionsData)
                        PartialGuildApplicationCommandPermissionsData.builder()
                                .id(commandIdsByName.computeIfAbsent(byCommandName.getKey(), k -> {
                                    throw new IllegalStateException(
                                            "Command name '" + byCommandName.getKey() + "' not found");
                                }))
                                .addAllPermissions(byCommandName.getValue().stream()
                                        .map(perm -> ApplicationCommandPermissionsData.builder()
                                                .id(perm.roleId())
                                                .type(1)
                                                .permission(true)
                                                .build())
                                        .collect(toUnmodifiableList()))
                                .build())
                .collect(toUnmodifiableList());
        return appService.bulkModifyApplicationCommandPermissions(applicationId, guildId, payload).then();
    }
}
