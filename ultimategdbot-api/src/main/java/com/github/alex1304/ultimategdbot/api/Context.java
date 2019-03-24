package com.github.alex1304.ultimategdbot.api;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Context of a bot command.
 */
public interface Context {
	/**
	 * Gets the command that created this context.
	 * 
	 * @return the original command
	 */
	Command getCommand();

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
	 * Gets the prefix used in the command that created this context.
	 * 
	 * @return the prefix used
	 */
	String getPrefixUsed();

	/**
	 * Sends a message in the same channel the command was sent.
	 * 
	 * @param message the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> reply(String message);

	/**
	 * Sends a message in the same channel the command was sent. This method
	 * supports advanced message construction.
	 * 
	 * @param message the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> reply(Consumer<? super MessageCreateSpec> spec);

	/**
	 * Adds a variable in this context. If a variable of the same name exists, it is
	 * overwritten.
	 * 
	 * @param name - the name of the variable
	 * @param val  - the value of the variable
	 */
	void setVar(String name, Object val);

	/**
	 * Adds a variable in this context. If a variable of the same name exists,
	 * nothing happens.
	 * 
	 * @param name - the name of the variable
	 * @param val  - the value of the variable
	 */
	void setVarIfNotExists(String name, Object val);

	/**
	 * Gets the value of a variable.
	 * 
	 * @param name the variable name
	 * @param type the type of the variable
	 * @return the value of the variable, or null if not found or exists in the
	 *         wrong type
	 */
	<T> T getVar(String name, Class<T> type);

	/**
	 * Gets the value of a variable. If not found, the provided default value is
	 * returned instead.
	 * 
	 * @param name       the variable name
	 * @param defaultVal the default value to return if not found
	 * @return the value of the variable, or the default value if not found or
	 *         exists in the wrong type
	 */
	<T> T getVarOrDefault(String name, T defaultVal);

	/**
	 * Gets the guild settings
	 * 
	 * @return an unmodifiable Map containing the guild settings keys and their
	 *         associated values.
	 */
	Mono<Map<Plugin, Map<String, String>>> getGuildSettings();

	/**
	 * Edits an entry of the guild settings.
	 * 
	 * @param key - the setting key
	 * @param val - the setting value
	 * @return a Mono that completes when successfully updated
	 * @throws NoSuchElementException        if no entry is found for the given key
	 * @throws IllegalArgumentException      if the given value is not accepted by
	 *                                       the entry
	 * @throws UnsupportedOperationException if this method is called in a context
	 *                                       that is outside of a Discord guild
	 */
	Mono<Void> setGuildSetting(String key, String val);

	/**
	 * Creates a new Context that is the exact copy of this one, but with
	 * different arguments.
	 * 
	 * @param newArgs  the new arguments that the copy will get
	 * @return a new Context
	 */
	Context fork(List<String> newArgs);
}
