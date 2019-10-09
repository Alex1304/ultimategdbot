package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;

import org.reactivestreams.Publisher;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.pool.InstrumentedPool;
import reactor.pool.PoolBuilder;

public class MutexPool {

	private final Cache<Object, InstrumentedPool<Object>> poolCache = Caffeine.newBuilder()
			.expireAfterAccess(Duration.ofMinutes(30))
			.build();
	
	public <X> Flux<X> acquireUntil(Object mutex, Publisher<X> until) {
		var pool = poolCache.asMap().computeIfAbsent(mutex, key -> PoolBuilder.from(Mono.fromCallable(Object::new))
					.sizeBetween(0, 1)
					.fifo());
		return pool.withPoolable(o -> until);
	}
}
