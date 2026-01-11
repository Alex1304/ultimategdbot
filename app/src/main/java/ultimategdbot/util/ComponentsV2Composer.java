package ultimategdbot.util;

import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ultimategdbot.component.CustomMessageComponent;

import java.util.Arrays;
import java.util.Collection;

public final class ComponentsV2Composer {

    public static Mono<MessageCreateSpec> composeMessage(Collection<? extends CustomMessageComponent> components) {
        return Mono.zip(Flux.fromIterable(components).map(CustomMessageComponent::defineComponent).collectList(),
                        Flux.fromIterable(components)
                                .flatMap(CustomMessageComponent::provideFiles)
                                .distinct(MessageCreateFields.File::name)
                                .collectList())
                .map(componentsAndFiles -> MessageCreateSpec.builder()
                        .components(componentsAndFiles.getT1())
                        .files(componentsAndFiles.getT2())
                        .build());
    }

    public static Mono<MessageCreateSpec> composeMessage(CustomMessageComponent... components) {
        return composeMessage(Arrays.asList(components));
    }
}
