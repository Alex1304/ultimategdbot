package com.github.alex1304.ultimategdbot.api;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.MessageCreateSpec;
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
	 * Gets the channel where the bot sends messages for debugging purposes.
	 * 
	 * @return a Mono emitting the debug log channel
	 */
	Mono<Channel> getDebugLogChannel();
	
	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param message - the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(String message);
	
	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param spec - the spec of the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(Consumer<MessageCreateSpec> spec);
	
	/**
	 * Gets the String representation of an emoji installed on one of the emoji servers.
	 * If the emoji is not found, the returned value is the given name wrapped in colons.
	 * 
	 * @param emojiName - the name of the emoji to look for
	 * @return the emoji code corresponding to the given name
	 */
	Mono<String> getEmoji(String emojiName);
	
	/**
	 * Gets the maximum time in seconds that the bot should wait for a reply when a reply menu is open.
	 * 
	 * @return the value as int (in seconds)
	 * @see Bot#openReplyMenu(Context, Message, Map, boolean, boolean)
	 */
	int getReplyMenuTimeout();
	
	/**
	 * Starts the bot.
	 */
	void start();

	/**
	 * Opens a new reply menu with the given items.
	 * 
	 * @param ctx           - the context of the command this reply menu was opened
	 *                      from
	 * @param msg           - The message containing the menu
	 * @param menuItems     - the menu items
	 * @param deleteOnReply - Whether to delete {@code msg} when the user has given
	 *                      a valid reply
	 * @param deleteOnTimeout - Whether to delete {@code msg} when the user doesn't
	 *                      reply and the menu times out.
	 * @return the identifier of the opened reply, or an empty string if the menu could not be opened.
	 */
	String openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout);
	
	/**
	 * Closes a reply menu using its identifier.
	 * 
	 * @param identifier - the identifier of the reply menu to close
	 */
	void closeReplyMenu(String identifier);
	
	/**
	 * Gets the command corresponding to the given name.
	 * 
	 * @param name - the name of the command
	 * @return the corresponding Command instance, or null if not found
	 */
	Command getCommandForName(String name);
	
	/**
	 * Returns the set of all available commands mapped by their respective plugins.
	 * 
	 * @return a Map that associates plugins with the commands they provide
	 */
	Map<Plugin, Set<Command>> getCommandsFromPlugins();

	/**
	 * Gets the guild settings entries loaded from plugins. Unlike
	 * {@link Context#getGuildSettings()}, this does not get the values for a
	 * specific guild, it gives functions to retrieve the values from any guild.
	 * 
	 * @return an unmodifiable Map containing the guild settings keys and their
	 *         associated values.
	 */
	Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> getGuildSettingsEntries();
}
