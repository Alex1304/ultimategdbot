package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;

import org.reactivestreams.Publisher;

import discord4j.rest.request.GlobalRateLimiter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UnboundedGlobalRateLimiter implements GlobalRateLimiter {
	
	private volatile long limitedUntil = 0;

	@Override
	public void rateLimitFor(Duration duration) {
		limitedUntil = System.nanoTime() + duration.toNanos();
	}

	@Override
	public Duration getRemaining() {
		return Duration.ofNanos(limitedUntil - System.nanoTime());
	}

	@Override
	public <T> Flux<T> withLimiter(Publisher<T> stage) {
		var remaining = getRemaining();
		var notifier = remaining.isNegative() || remaining.isZero() ? Mono.empty() : Mono.delay(remaining);
		return notifier.thenMany(stage);
	}

}
