package com.github.alex1304.ultimategdbot.api;

import java.util.List;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.entity.GuildSettings;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Context of a bot command.
 */
public interface Context {
	/**
	 * Gets the message create event associated to this command.
	 *
	 * @return the event
	 */
	MessageCreateEvent getEvent();

	/**
	 * Gets the arguments of the command
	 *
	 * @return the args
	 */
	List<String> getArgs();
	
	/**
	 * Gets the bot instance
	 * 
	 * @return the bot
	 */
	Bot getBot();
	
	/**
	 * Gets the settings for the guild this command has been run in.
	 * 
	 * @return the guild settings, or null if the command has been used in a DM.
	 */
	GuildSettings getGuildSettings();
	
	/**
	 * Sends a message in the same channel the command was sent.
	 * 
	 * @param message - the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> reply(String message);
	
	/**
	 * Sends a message in the same channel the command was sent. This method supports advanced message construction.
	 * 
	 * @param message - the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> reply(Consumer<? super MessageCreateSpec> spec);
}
