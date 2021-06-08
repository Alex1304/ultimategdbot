package ultimategdbot.event;

import botrino.api.i18n.Translator;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import ultimategdbot.Strings;

import java.time.Duration;

class CrosspostQueue {
	
	private static final Logger LOGGER = Loggers.getLogger(CrosspostQueue.class);
	
	private final Translator tr;

    CrosspostQueue(Translator tr) {
        this.tr = tr;
    }

    void submit(Message message, Object event) {
		var crosspostCompletion = Sinks.empty();
		var warnDelayed = Mono.firstWithSignal(crosspostCompletion.asMono(), Mono
                .delay(Duration.ofSeconds(10))
				.flatMap(__ -> message.getChannel())
				.flatMap(channel -> channel.createMessage(":warning: " + tr
						.translate(Strings.GD, "gdevents_crosspost_delayed"))));
		var doCrosspost = message.publish()
                .then(Mono.fromRunnable(() -> crosspostCompletion.emitEmpty(
		                (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED)));
		Mono.when(doCrosspost, warnDelayed).subscribe(null,
				t -> LOGGER.error("Unable to crosspost message for event " + event, t),
				() -> LOGGER.info("Successfully crossposted message for event {}", event));
	}
}
