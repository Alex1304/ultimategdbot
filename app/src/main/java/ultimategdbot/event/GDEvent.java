package ultimategdbot.event;

import botrino.api.util.MessageTemplate;
import discord4j.rest.entity.RestChannel;
import org.immutables.value.Value;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Value.Immutable
public interface GDEvent<E> {

    Function<E, RestChannel> channel();

    Function<E, Optional<Long>> levelIdGetter();

    Function<E, Mono<Long>> recipientAccountId();

    Function<E, Mono<MessageTemplate>> messageTemplateFactory();

    Function<E, String> congratMessage();

    boolean isUpdate();

    @SuppressWarnings("unchecked")
    default RestChannel channel(Object event) {
        return channel().apply((E) event);
    }

    @SuppressWarnings("unchecked")
    default Optional<Long> levelId(Object event) {
        return levelIdGetter().apply((E) event);
    }

    @SuppressWarnings("unchecked")
	default Mono<Long> recipientAccountId(Object event) {
		return recipientAccountId().apply((E) event);
	}

	@SuppressWarnings("unchecked")
	default Mono<MessageTemplate> createMessageTemplate(Object event) {
		return messageTemplateFactory().apply((E) event);
	}

	@SuppressWarnings("unchecked")
	default String congratMessage(Object event) {
		return congratMessage().apply((E) event);
	}
}
