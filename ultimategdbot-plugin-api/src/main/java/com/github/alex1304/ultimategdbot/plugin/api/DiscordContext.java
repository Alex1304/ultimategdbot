package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.List;

import discord4j.core.event.domain.message.MessageCreateEvent;

/**
 * Context for Discord bot commands. You can also execute other commands using
 * the context of the current object.
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
