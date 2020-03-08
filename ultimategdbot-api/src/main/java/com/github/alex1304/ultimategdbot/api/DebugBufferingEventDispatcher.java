package com.github.alex1304.ultimategdbot.api;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
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
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;

public class DebugBufferingEventDispatcher implements EventDispatcher {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugBufferingEventDispatcher.class);

	private final UnicastProcessor<Event> processorIn;
	private final EmitterProcessor<Event> processorOut;
	private final FluxSink<Event> sink;
	private final Scheduler scheduler;
	private final ConcurrentHashMap<Class<? extends Event>, Integer> activeSubscriptions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<? extends Event>, Integer> processedEvents = new ConcurrentHashMap<>();
	

	public DebugBufferingEventDispatcher(Scheduler scheduler) {
		this.processorIn = UnicastProcessor.create();
		this.processorOut = processorIn.subscribeWith(EmitterProcessor.create(false));
		this.sink = processorIn.sink(FluxSink.OverflowStrategy.BUFFER);
		this.scheduler = requireNonNull(scheduler);
	}

	@Override
	public <T extends Event> Flux<T> on(Class<T> eventClass) {
		var subscription = new AtomicReference<Subscription>();
		return processorOut.publishOn(scheduler)
				.ofType(eventClass)
				.doOnSubscribe(sub -> {
					subscription.set(sub);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Subscription {} to {} created", Integer.toHexString(sub.hashCode()),
								eventClass.getSimpleName());
					}
					activeSubscriptions.merge(eventClass, 1, Integer::sum);
                })
				.doOnNext(event -> processedEvents.merge(eventClass, 1, Integer::sum))
                .doFinally(signal -> {
					var sub = subscription.get();
					if (sub != null && LOGGER.isDebugEnabled()) {
						LOGGER.debug("Subscription {} to {} disposed due to {}", Integer.toHexString(sub.hashCode()),
								eventClass.getSimpleName(), signal);
					}
					activeSubscriptions.merge(eventClass, 1, this::subtractOrRemove);
                });
	}

	@Override
	public void publish(Event event) {
		sink.next(event);
	}

	@Override
	public void shutdown() {
		sink.complete();
		processorOut.onComplete();
	}
	
	public int getEventQueueSize() {
		return processorIn.size();
	}
	
	public Map<Class<? extends Event>, Integer> getActiveSubscriptionsView() {
		return Collections.unmodifiableMap(activeSubscriptions);
	}
	
	public Map<Class<? extends Event>, Integer> getProcessedEventsView() {
		return Collections.unmodifiableMap(processedEvents);
	}
	
	private Integer subtractOrRemove(Integer oldValue, Integer increment) {
		if (oldValue <= increment) {
			return null;
		}
		return oldValue - increment;
	}
}
