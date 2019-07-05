package com.github.alex1304.ultimategdbot.api;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.rest.request.GlobalRateLimiter;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * <p>
 * Custom implementation of {@link GlobalRateLimiter} that uses a clock ticking
 * at regular intervals in order to give permits for requests.
 * 
 * <p>
 * For example, if the clock frequency is set to 25 ticks per second, this
 * limiter will allow a maximum throughput of 25 requests per second.
 * </p>
 * 
 * <p>
 * The effective throughput may be lower than the specified one if Discord's
 * global rate limit is being reached.
 * </p>
 * 
 * @author Alex1304
 */
public class ClockRateLimiter implements GlobalRateLimiter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot.globalratelimiter");
	
	private final AtomicLong stageIds;
	private final EmitterProcessor<Long> clock;
	private final AtomicLong lastAssignedTick;
	private final AtomicLong lastElapsedTick;
	private final AtomicBoolean isGloballyRateLimited;
	private final AtomicLong limitedUntil;
	
	/**
	 * Creates a {@link ClockRateLimiter} with a specified clock frequency.
	 * 
	 * @param frequency the target frequency at which permits should be given
	 */
	public ClockRateLimiter(int frequency) {
		if (frequency < 1) {
			throw new IllegalArgumentException("frequency must be >= 1");
		}
		this.stageIds = new AtomicLong();
		this.clock = EmitterProcessor.create(false);
		this.lastAssignedTick = new AtomicLong();
		this.lastElapsedTick = new AtomicLong();
		this.isGloballyRateLimited = new AtomicBoolean();
		this.limitedUntil = new AtomicLong();
		Flux.interval(Duration.ofNanos(1_000_000_000 / frequency))
				.doOnNext(lastElapsedTick::set)
				.subscribeWith(clock)
				.subscribe();
	}

	@Override
	public void rateLimitFor(Duration duration) {
		isGloballyRateLimited.set(true);
		limitedUntil.set(System.nanoTime() + duration.toNanos());
		Mono.delay(duration, Schedulers.elastic())
				.doOnNext(__ -> isGloballyRateLimited.set(false))
				.subscribe();
	}
	
	@Override
	public Duration getRemaining() {
		var remaining = limitedUntil.get() - System.nanoTime();
		var duration = Duration.ofNanos(remaining);
		if (remaining > 0) {
			LOGGER.debug("On hold for {}", duration);
		}
		return duration;
	}
	
	@Override
	public <T> Flux<T> withLimiter(Publisher<T> stage) {
		var stageId = stageIds.incrementAndGet();
		var targetTick = new AtomicLong();
		synchronized (lastAssignedTick) {
			targetTick.set(Math.max(lastElapsedTick.get(), lastAssignedTick.get()) + 1);
			lastAssignedTick.set(targetTick.get());
			LOGGER.debug("Stage #{} is targeting tick: {}", stageId, targetTick);
		}
		return clock.skipUntil(tick -> {
					if (tick >= targetTick.get()) {
						if (!isGloballyRateLimited.get()) {
							LOGGER.debug("Stage #{} has reached target tick {} and has received permit", stageId, tick);
							return true;
						} else {
							synchronized (lastAssignedTick) {
								targetTick.set(Math.max(lastElapsedTick.get(), lastAssignedTick.get()) + 1);
								lastAssignedTick.set(targetTick.get());
							}
							LOGGER.debug("Stage #{} has reached target tick {} but is globally rate limited. "
									+ "New target set to {}", stageId, tick, targetTick.get());
							return false;
						}
					} else {
						return false;
					}
				})
				.next()
				.thenMany(stage);
	}

}
