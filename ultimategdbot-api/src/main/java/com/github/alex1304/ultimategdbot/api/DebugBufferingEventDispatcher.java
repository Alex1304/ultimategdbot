package com.github.alex1304.ultimategdbot.api;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.util.BotUtils;

import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;

public class DebugBufferingEventDispatcher implements EventDispatcher {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DebugBufferingEventDispatcher.class);
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final UnicastProcessor<EventWithTime> processorIn;
	private final EmitterProcessor<EventWithTime> processorOut;
	private final FluxSink<EventWithTime> sink;
	private final Scheduler scheduler;
	

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
				.filter(eventWithTime -> {
					var elapsed = Duration.ofNanos(scheduler.now(TimeUnit.NANOSECONDS) - eventWithTime.publishTimeNanos);
					if (!elapsed.minus(TIMEOUT).isNegative()) {
						LOGGER.warn("Ignoring {}, took too long to be consumed ({}). Total events in queue: {}",
								LOGGER.isTraceEnabled()
										? eventWithTime.event.toString()
										: eventWithTime.event.getClass().getSimpleName(),
								BotUtils.formatDuration(elapsed),
								processorIn.size());
						return false;
					}
					return true;
				})
				.map(EventWithTime::getEvent)
				.ofType(eventClass)
				.doOnSubscribe(sub -> {
					subscription.set(sub);
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Subscription {} to {} created", Integer.toHexString(sub.hashCode()),
								eventClass.getSimpleName());
					}
                })
                .doFinally(signal -> {
					var sub = subscription.get();
					if (sub != null && LOGGER.isDebugEnabled()) {
						LOGGER.debug("Subscription {} to {} disposed due to {}", Integer.toHexString(sub.hashCode()),
								eventClass.getSimpleName(), signal);
					}
                });
	}

	@Override
	public void publish(Event event) {
		sink.next(new EventWithTime(event, scheduler.now(TimeUnit.NANOSECONDS)));
	}

	@Override
	public void shutdown() {
		sink.complete();
		processorOut.onComplete();
	}
	
	private static class EventWithTime {
		private final Event event;
		private final long publishTimeNanos;
		
		private EventWithTime(Event event, long publishTimeNanos) {
			this.event = event;
			this.publishTimeNanos = publishTimeNanos;
		}

		private Event getEvent() {
			return event;
		}
	}
}