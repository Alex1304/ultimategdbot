package com.github.alex1304.ultimategdbot.api.command.menu;

import java.util.Optional;
import java.util.function.Function;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;

public final class ReactionToggleEvent {

	private final Optional<ReactionAddEvent> addEvent;
	private final Optional<ReactionRemoveEvent> removeEvent;
	
	public ReactionToggleEvent(Event event) {
		this.addEvent = Optional.of(event)
				.filter(ReactionAddEvent.class::isInstance)
				.map(ReactionAddEvent.class::cast);
		this.removeEvent = Optional.of(event)
				.filter(ReactionRemoveEvent.class::isInstance)
				.map(ReactionRemoveEvent.class::cast);
		if (addEvent.isEmpty() && removeEvent.isEmpty()) {
			throw new IllegalArgumentException("Must be either ReactionAddEvent or ReactionRemoveEvent");
		}
	}
	
	private <T> T takeEither(Function<ReactionAddEvent, T> fromAddEvent, Function<ReactionRemoveEvent, T> fromRemoveEvent) {
        return addEvent.map(fromAddEvent::apply)
        		.or(() -> removeEvent.map(fromRemoveEvent::apply))
        		.orElseThrow();
	}
	
    public Snowflake getUserId() {
        return takeEither(ReactionAddEvent::getUserId, ReactionRemoveEvent::getUserId);
    }

    public Mono<User> getUser() {
        return takeEither(ReactionAddEvent::getUser, ReactionRemoveEvent::getUser);
    }

    public Snowflake getChannelId() {
        return takeEither(ReactionAddEvent::getChannelId, ReactionRemoveEvent::getChannelId);
    }

    public Mono<MessageChannel> getChannel() {
        return takeEither(ReactionAddEvent::getChannel, ReactionRemoveEvent::getChannel);
    }

    public Snowflake getMessageId() {
        return takeEither(ReactionAddEvent::getMessageId, ReactionRemoveEvent::getMessageId);
    }

    public Mono<Message> getMessage() {
        return takeEither(ReactionAddEvent::getMessage, ReactionRemoveEvent::getMessage);
    }

    public Optional<Snowflake> getGuildId() {
        return takeEither(ReactionAddEvent::getGuildId, ReactionRemoveEvent::getGuildId);
    }

    public Mono<Guild> getGuild() {
        return takeEither(ReactionAddEvent::getGuild, ReactionRemoveEvent::getGuild);
    }

    public ReactionEmoji getEmoji() {
        return takeEither(ReactionAddEvent::getEmoji, ReactionRemoveEvent::getEmoji);
    }
    
    public boolean isAddEvent() {
    	return addEvent.isPresent();
    }
}
