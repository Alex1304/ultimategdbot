package com.github.alex1304.ultimategdbot.api;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.util.concurrent.Queues;

public class DebugBufferingEventDispatcher implements EventDispatcher {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugBufferingEventDispatcher.class);

	private final EmitterProcessor<Event> processor;
	private final FluxSink<Event> sink;
	private final Scheduler scheduler;
	private final ConcurrentHashMap<Class<? extends Event>, Integer> subscribedEventTypes = new ConcurrentHashMap<>();

	public DebugBufferingEventDispatcher(Scheduler scheduler) {
		this.processor = EmitterProcessor.create(Queues.SMALL_BUFFER_SIZE, false);
		this.sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
		this.scheduler = requireNonNull(scheduler);
	}

	@Override
	public <T extends Event> Flux<T> on(Class<T> eventClass) {
		var subscription = new AtomicReference<Subscription>();
		return processor.publishOn(scheduler)
				.ofType(eventClass)
				.doOnSubscribe(sub -> {
                    subscription.set(sub);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Subscription {} to {} created", Integer.toHexString(sub.hashCode()),
                                eventClass.getSimpleName());
                    }
                    subscribedEventTypes.merge(eventClass, 1, Integer::sum);
                })
                .doFinally(signal -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Subscription {} to {} disposed due to {}",
                                Integer.toHexString(subscription.get().hashCode()), eventClass.getSimpleName(), signal);
                    }
                    subscribedEventTypes.merge(eventClass, 1, this::subtractOrRemove);
                });
	}

	@Override
	public void publish(Event event) {
		sink.next(event);
		if (subscribedEventTypes.getOrDefault(event.getClass(), 0) > 0) {
			LOGGER.debug("Published event {}", event);
			var pending = processor.getPending();
			LOGGER.debug("There {} now {} pending event{} in queue",
					pending,
					pending > 1 ? "are": "is",
					pending > 1 ? "s" : "");
		}
	}

	@Override
	public void shutdown() {
		sink.complete();
	}
	
	private Integer subtractOrRemove(Integer oldValue, Integer increment) {
		if (oldValue <= increment) {
			return null;
		}
		return oldValue - increment;
	}
}