package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.rest.request.GlobalRateLimiter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>Custom implementation of {@link GlobalRateLimiter} that accepts a throughput
 * parameter, and adjusts the delay of requests in a such way that the effective
 * throughput complies with the targeted one.</p>
 * 
 * <p>For example, if the parameter is set to 25, this limiter will allow a maximum
 * throughput of 25 requests per second.</p>
 * 
 * <p>The effective throughput may be lower than the specified one if Discord's
 * global rate limit is being reached.</p>
 * 
 * @author Alex1304
 */
public class FixedThroughputGlobalRateLimiter implements GlobalRateLimiter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot.globalratelimiter");
	
	private final long delayStepNanos;
	private volatile long globallyRateLimitedUntil = 0;
	private volatile long throughputLimitedUntil = 0;
	
	/**
	 * Creates a {@link FixedThroughputGlobalRateLimiter} with a specific throughput value.
	 * 
	 * @param targetThroughput the target thoughput value
	 */
	public FixedThroughputGlobalRateLimiter(int targetThroughput) {
		if (targetThroughput < 1) {
			throw new IllegalArgumentException("throughput must be >= 1");
		}
		this.delayStepNanos = 1_000_000_000 / targetThroughput;
	}

	/**
	 * Notifies when the request is ready to be sent.
	 * 
	 * @return a Mono that completes when this limiter is ready to allow for more requests
	 */
	private synchronized Mono<Void> notifier() {
		var now = System.nanoTime();
		throughputLimitedUntil = Math.max(throughputLimitedUntil + delayStepNanos, now);
		return Mono.delay(Duration.ofNanos(Math.max(globallyRateLimitedUntil, throughputLimitedUntil) - now))
				.then(Mono.fromRunnable(() -> LOGGER.debug("Permit!")));
	}

	@Override
	public void rateLimitFor(Duration duration) {
		globallyRateLimitedUntil = System.nanoTime() + duration.toNanos();
	}
	
	@Override
	public Duration getRemaining() {
		var remaining = globallyRateLimitedUntil - System.nanoTime();
		var duration = Duration.ofNanos(remaining);
		if (remaining > 0) {
			LOGGER.debug("On hold for {}", duration);
		}
		return duration;
	}
	
	@Override
	public <T> Flux<T> withLimiter(Publisher<T> stage) {
		return Flux.defer(() -> notifier().thenMany(stage));
	}

}
