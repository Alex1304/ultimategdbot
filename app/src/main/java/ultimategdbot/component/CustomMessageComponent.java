package ultimategdbot.component;

import discord4j.core.object.component.TopLevelMessageComponent;
import discord4j.core.spec.MessageCreateFields;
import reactor.core.publisher.Flux;

@FunctionalInterface
public interface CustomMessageComponent {

    TopLevelMessageComponent defineComponent();

    default Flux<? extends MessageCreateFields.File> provideFiles() {
        return Flux.empty();
    }
}
