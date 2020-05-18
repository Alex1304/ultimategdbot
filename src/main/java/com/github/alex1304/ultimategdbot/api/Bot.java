package com.github.alex1304.ultimategdbot.api;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.CommandKernel;
import com.github.alex1304.ultimategdbot.api.database.Database;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigDao;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigurator;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself.
 */
public interface Bot {
	
	/**
	 * Gets the config of the bot.
	 * 
	 * @return the config
	 */
	public BotConfig config();
	
	/**
	 * Gets the properties of the bot.
	 * 
	 * @return the properties
	 */
	public PropertyReader properties();

	/**
	 * Gets the database of the bot.
	 * 
	 * @return the database
	 */
	public Database database();

	/**
	 * Gets the command kernel of this bot.
	 * 
	 * @return the command kernel
	 */
	public CommandKernel commandKernel();

	/**
	 * Gets the REST client of the bot.
	 * 
	 * @return the Discord client
	 */
	public DiscordClient rest();

	/**
	 * Gets the gateway client of the bot.
	 * 
	 * @return the gateway client
	 */
	public GatewayDiscordClient gateway();

	/**
	 * Gets a Set containing all successfully loaded plugins.
	 * 
	 * @return a Set of Plugin
	 */
	public Set<Plugin> plugins();
	
	/**
	 * Gets the bot owner.
	 * 
	 * @return a Mono emitting the bot owner
	 */
	public Mono<User> owner();
	
	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param message the message to send
	 * @return a Mono completing when the log message is sent
	 */
	public Mono<Void> log(String message);
	
	/**
	 * Gets the String representation of an emoji installed on one of the emoji
	 * servers. If the emoji is not found, the returned value is the given name
	 * wrapped in colons.
	 * 
	 * @param emojiName the name of the emoji to look for
	 * @return a Mono emitting the emoji code corresponding to the given name
	 */
	public Mono<String> emoji(String emojiName);
	
	/**
	 * Retrieves all registered configurators for the given guild referenced by its
	 * ID. Empty configuration data will be inserted in database for this specific
	 * guild if it doesn't exist yet.
	 * 
	 * @param guildId the guild ID
	 * @return a Flux emitting all configurators for the guild
	 */
	Flux<GuildConfigurator<?>> configureGuild(Snowflake guildId);
	
	/**
	 * Registers a guild configuration extension to this database. This allows to
	 * retrieve all configuration data via the {@link #configureGuild(Snowflake)}
	 * method.
	 * 
	 * @param extension the extension class to register
	 */
	void registerGuildConfigExtension(Class<? extends GuildConfigDao<?>> extension);

	/**
	 * Starts the bot. This method blocks until the bot disconnects.
	 */
	public void start();
}
