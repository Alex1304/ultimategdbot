package ultimategdbot.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.core.object.entity.Message;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class BroadcastResultCache {
	
	private final Cache<Long, List<Message>> results = Caffeine.newBuilder()
			.maximumSize(50)
			.build();
	
	void put(long levelId, List<Message> messages) {
		requireNonNull(messages);
		results.put(levelId, messages);
	}
	
	Optional<List<Message>> get(long levelId) {
		return Optional.ofNullable(results.getIfPresent(levelId))
				.map(Collections::unmodifiableList);
	}
}
