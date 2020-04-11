package com.github.alex1304.ultimategdbot.core;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Optional;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

class MemoryStats {
	private static final ReplayProcessor<MemoryStats> STATS = ReplayProcessor.cacheLastOrDefault(new MemoryStats());
	private static final FluxSink<MemoryStats> STATS_SINK = STATS.sink(FluxSink.OverflowStrategy.LATEST);
	
	private final long timestamp;
	final long totalMemory;
	final long usedMemory;
	final long maxMemory;
	
	private MemoryStats(long timestamp) {
		var total = Runtime.getRuntime().totalMemory();
		var free = Runtime.getRuntime().freeMemory();
		var max = Runtime.getRuntime().maxMemory();
		this.timestamp = timestamp;
		this.totalMemory = total;
		this.usedMemory = total - free;
		this.maxMemory = max;
	}
	
	private MemoryStats() {
		this.timestamp = -1;
		this.totalMemory = Runtime.getRuntime().totalMemory();
		this.usedMemory = 0;
		this.maxMemory = Runtime.getRuntime().maxMemory();
	}

	Optional<Duration> elapsedSinceLastGC() {
		return Optional.of(timestamp)
				.filter(t -> t > 0)
				.map(t -> Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime() - t));
	}
	
	static Mono<MemoryStats> getStats() {
		return STATS.next();
	}
	
	static void start() {
		Flux.<MemoryStats>create(sink -> {
			NotificationListener gcListener = (notif, handback) -> {
				if (notif.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
					var gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notif.getUserData()).getGcInfo();
					sink.next(new MemoryStats(gcInfo.getEndTime()));
				}
			};
			ManagementFactory.getGarbageCollectorMXBeans()
					.forEach(bean -> ((NotificationEmitter) bean).addNotificationListener(gcListener, null, null));
		}).subscribe(STATS_SINK::next);
	}
}