package com.github.alex1304.ultimategdbot.api;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
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
	 * Starts the bot.
	 */
	void start();
}
