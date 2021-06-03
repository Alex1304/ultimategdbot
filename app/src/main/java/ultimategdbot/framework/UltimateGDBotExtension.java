package ultimategdbot.framework;

import botrino.api.config.ConfigContainer;
import botrino.api.extension.BotrinoExtension;
import botrino.command.Command;
import botrino.command.CommandService;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import jdash.client.GDClient;
import jdash.graphics.SpriteFactory;
import reactor.core.publisher.Mono;
import ultimategdbot.command.SetupCommand;
import ultimategdbot.command.SetupEntry;
import ultimategdbot.service.ExternalServices;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.github.alex1304.rdi.ServiceReference.ofType;
import static com.github.alex1304.rdi.config.FactoryMethod.externalStaticFactory;
import static com.github.alex1304.rdi.config.Injectable.ref;

public final class UltimateGDBotExtension implements BotrinoExtension {

    private final Set<Command> setupEntries = new HashSet<>();
    private SetupCommand setupCommand;
    private CommandService commandService;

    @Override
    public void onClassDiscovered(Class<?> clazz) {
    }

    @Override
    public void onServiceCreated(Object serviceInstance) {
        if (serviceInstance instanceof Command && serviceInstance.getClass().isAnnotationPresent(SetupEntry.class)) {
            setupEntries.add((Command) serviceInstance);
        }
        if (serviceInstance instanceof CommandService) {
            this.commandService = (CommandService) serviceInstance;
        }
        if (serviceInstance instanceof SetupCommand) {
            this.setupCommand = (SetupCommand) serviceInstance;
        }
    }

    @Override
    public Set<ServiceDescriptor> provideExtraServices() {
        return Set.of(
                ServiceDescriptor.builder(ofType(ApplicationInfo.class))
                        .setFactoryMethod(externalStaticFactory(ExternalServices.class, "applicationInfo",
                                Mono.class, ref(ofType(GatewayDiscordClient.class))))
                        .build(),
                ServiceDescriptor.builder(ofType(GDClient.class))
                        .setFactoryMethod(externalStaticFactory(ExternalServices.class, "gdClient",
                                Mono.class, ref(ofType(ConfigContainer.class))))
                        .build(),
                ServiceDescriptor.builder(ofType(SpriteFactory.class))
                        .setFactoryMethod(externalStaticFactory(ExternalServices.class, "spriteFactory", Mono.class))
                        .build()
        );
    }

    @Override
    public Set<Class<?>> provideExtraDiscoverableClasses() {
        return Set.of();
    }

    @Override
    public Mono<Void> finishAndJoin() {
        return Mono.fromRunnable(() -> {
            Objects.requireNonNull(setupCommand);
            Objects.requireNonNull(commandService);
            setupCommand.addAllSetupEntries(setupEntries);
            commandService.addTopLevelCommand(setupCommand);
        });
    }
}
