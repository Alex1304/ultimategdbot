package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.List;
import java.util.Objects;

import discord4j.core.event.domain.message.MessageCreateEvent;

/**
 * Context for Discord bot commands. You can also execute other commands using
 * the context of the current object.
 *
 * @author Alex1304
 *
 */
public class DiscordContext {
	
	private final Bot bot;
	private final MessageCreateEvent event;
	private final List<String> args;
	private final String prefixUsed;
	private final String commandName;

	public DiscordContext(Bot bot, MessageCreateEvent event, List<String> args, String prefixUsed, String commandName) {
		this.bot = Objects.requireNonNull(bot);
		this.event = Objects.requireNonNull(event);
		this.args = Objects.requireNonNull(args);
		this.prefixUsed = Objects.requireNonNull(prefixUsed);
		this.commandName = Objects.requireNonNull(commandName);
	}
	
	/**
	 * Contructs an exact copy the given DiscordContext with only the arguments changing
	 * 
	 * @param original - the original context
	 * @param newArgs - new arguments
	 */
	public DiscordContext(DiscordContext original, List<String> newArgs) {
		this(Objects.requireNonNull(original).bot, original.event, Objects.requireNonNull(newArgs), original.prefixUsed, original.commandName);
	}

	/**
	 * Gets the event
	 *
	 * @return MessageCreateEvent
	 */
	public MessageCreateEvent getEvent() {
		return event;
	}

	/**
	 * Gets the args
	 *
	 * @return List<String>
	 */
	public List<String> getArgs() {
		return args;
	}

	/**
	 * Gets the bot
	 * 
	 * @return UltimateGDBot
	 */
	public Bot getBot() {
		return bot;
	}

	/**
	 * Gets the prefix used
	 * 
	 * @return String
	 */
	public String getPrefixUsed() {
		return prefixUsed;
	}

	/**
	 * Gets the command name
	 * 
	 * @return String
	 */
	public String getCommandName() {
		return commandName;
	}
}
