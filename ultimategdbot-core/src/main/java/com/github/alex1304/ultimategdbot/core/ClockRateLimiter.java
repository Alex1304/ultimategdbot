package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.rest.request.GlobalRateLimiter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.retry.BackoffDelay;
import reactor.retry.Retry;

/**
 * <p>
 * Custom implementation of {@link GlobalRateLimiter} that uses a clock ticking
 * at regular intervals in order to give permits for requests.
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
	
	private final AtomicLong requestIdGenerator;
	private final AtomicLong limitedUntil;
	private final AtomicInteger permitsRemaining;
	private final AtomicLong permitsResetAfter;
	
	/**
	 * Creates a {@link ClockRateLimiter} with a specified interval and number of
	 * permits per tick.
	 * 
	 * @param permitsPerTick the max number of requests per tick
	 * @param interval       the interval between two clock ticks
	 */
	public ClockRateLimiter(int permitsPerTick, Duration interval) {
		if ((Objects.requireNonNull(interval)).isNegative() || interval.isZero()) {
			throw new IllegalArgumentException("interval must be a non-zero positive duration");
		}
		this.requestIdGenerator = new AtomicLong();
		this.limitedUntil = new AtomicLong();
		this.permitsRemaining = new AtomicInteger();
		this.permitsResetAfter = new AtomicLong();
		Flux.interval(interval, Schedulers.elastic())
				.doOnNext(tick -> permitsRemaining.set(permitsPerTick))
				.doOnNext(tick -> permitsResetAfter.set(System.nanoTime() + interval.toNanos()))
				.subscribe();
	}

	@Override
	public void rateLimitFor(Duration duration) {
		limitedUntil.set(System.nanoTime() + duration.toNanos());
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
		var reqId = requestIdGenerator.incrementAndGet();
		var retryIn = new AtomicLong();
		return Mono.create(sink -> {
					retryIn.set(0);
					var now = System.nanoTime();
					if (permitsRemaining.decrementAndGet() < 0) {
						retryIn.set(permitsResetAfter.get() - now);
					}
					if (now < limitedUntil.get()) {
						retryIn.set(Math.max(retryIn.get(), limitedUntil.get() - now));
					}
					if (retryIn.get() > 0) {
						sink.error(new RuntimeException());
					} else {
						sink.success();
					}
				})
				.retryWhen(Retry.any()
						.doOnRetry(ctx -> LOGGER.debug("Request #{}: Delayed for {}", reqId, Duration.ofNanos(retryIn.get())))
						.backoff(ctx -> new BackoffDelay(Duration.ofNanos(retryIn.get()))))
				.then(Mono.fromRunnable(() -> LOGGER.debug("Request #{}: Permit!", reqId)))
				.thenMany(stage);
	}

}
