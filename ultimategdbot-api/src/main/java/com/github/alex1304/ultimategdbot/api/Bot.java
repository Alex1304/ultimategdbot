package com.github.alex1304.ultimategdbot.api;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself.
 */
public interface Bot {
	/**
	 * Get the bot token.
	 * 
	 * @return the token
	 */
	String getToken();
	
	/**
	 * Gets the default prefix.
	 * 
	 * @return the default prefix
	 */
	String getDefaultPrefix();
	
	/**
	 * Gets the support server of the bot.
	 * 
	 * @return the support server
	 */
	Mono<Guild> getSupportServer();
	
	/**
	 * Gets the moderator role of the bot.
	 * 
	 * @return the moderator role
	 */
	Mono<Role> getModeratorRole();
	
	/**
	 * Gets the release channel of the bot.
	 * 
	 * @return the release channel
	 */
	String getReleaseChannel();
	
	/**
	 * Gets the discord client.
	 * 
	 * @return the discord client
	 */
	DiscordClient getDiscordClient();
	
	/**
	 * Gets the database of the bot.
	 * 
	 * @return the database
	 */
	Database getDatabase();
	
	/**
	 * Gets the available commands loaded on the bot.
	 * 
	 * @return an unmodifiable Set of commands
	 */
	Set<Command> getAvailableCommands();
	
	/**
	 * Starts the bot.
	 */
	void start();

	/**
	 * Opens a new reply menu with the given items.
	 * 
	 * @param ctx       - the context of the command this reply menu was opened from
	 * @param msg       - The message containing the menu
	 * @param menuItems - the menu items
	 */
	void openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems);
}
