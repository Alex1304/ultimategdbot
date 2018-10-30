package com.github.alex1304.ultimategdbot.plugin.api;

import discord4j.core.spec.MessageCreateSpec;

/**
 * Interface that bot commands should implement. A command takes a context in
 * parameter and returns a view.
 *
 * @author Alex1304
 * 
 */
@FunctionalInterface
public interface Command extends Plugin {

	/**
	 * Executes the command
	 * 
	 * @param ctx - the context
	 * @return a View
	 * @throws CommandFailedException if something goes wrong when executing the
	 *                                command
	 */
	MessageCreateSpec execute(DiscordContext ctx) throws CommandFailedException;

	/**
	 * Gets the name of the command. By default, it is the name of the class
	 * implementing the interface in lowercase minus the Command suffix if there is
	 * any. For example, it would return {@code ping} if the class is named
	 * {@code Ping} or {@code PingCommand}. This method shouldn't return an empty
	 * string. So, if the class is named {@code Command}, the output would be
	 * {@code Command}.
	 * 
	 * @return a String
	 */
	@Override
	default String getName() {
		final var classname = this.getClass().getSimpleName().toLowerCase();
		final var suffix = "command";

		if (!classname.equals(suffix) && classname.endsWith(suffix))
			return classname.substring(0, classname.length() - suffix.length());

		return classname;
	}
}
