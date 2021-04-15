package ultimategdbot.framework;

import botrino.api.extension.BotrinoExtension;
import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import reactor.core.publisher.Mono;
import ultimategdbot.service.ExternalServices;

import java.util.Set;

import static com.github.alex1304.rdi.config.FactoryMethod.externalStaticFactory;
import static com.github.alex1304.rdi.config.Injectable.ref;

public final class UltimateGDBotExtension implements BotrinoExtension {

    @Override
    public void onClassDiscovered(Class<?> clazz) {

    }

    @Override
    public void onServiceCreated(Object serviceInstance) {
    }

    @Override
    public Set<ServiceDescriptor> provideExtraServices() {
        return Set.of(ServiceDescriptor.builder(ServiceReference.ofType(ApplicationInfo.class))
                .setFactoryMethod(externalStaticFactory(ExternalServices.class, "applicationInfo",
                        Mono.class, ref(ServiceReference.ofType(GatewayDiscordClient.class))))
                .build());
    }

    @Override
    public Set<Class<?>> provideExtraDiscoverableClasses() {
        return Set.of();
    }

    @Override
    public Mono<Void> finishAndJoin() {
        return Mono.empty();
    }
}
