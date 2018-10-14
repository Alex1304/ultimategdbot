package com.github.alex1304.ultimategdbot.command.api;

import java.util.List;

import discord4j.core.event.domain.message.MessageCreateEvent;

/**
 * Context for Discord bot commands
 *
 * @author Alex1304
 *
 */
public class DiscordContext {
	
	private final MessageCreateEvent event;
	private final List<String> args;
	
	public DiscordContext(MessageCreateEvent event, List<String> args) {
		this.event = event;
		this.args = args;
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
}
