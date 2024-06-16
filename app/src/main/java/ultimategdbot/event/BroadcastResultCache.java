package ultimategdbot.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class BroadcastResultCache {
	
	private final Cache<Long, List<MessageId>> results = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofDays(3))
			.build();
	
	void put(long levelId, List<MessageId> messages) {
		requireNonNull(messages);
		results.put(levelId, messages);
	}
	
	Optional<List<MessageId>> get(long levelId) {
		return Optional.ofNullable(results.getIfPresent(levelId))
				.map(Collections::unmodifiableList);
	}

    record MessageId(Snowflake channelId, Snowflake messageId) {
        Mono<Message> toMessage(GatewayDiscordClient gateway) {
            return gateway.getMessageById(channelId, messageId);
        }
    }
}
