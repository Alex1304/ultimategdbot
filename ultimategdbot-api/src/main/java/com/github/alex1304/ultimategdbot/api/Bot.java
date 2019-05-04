package com.github.alex1304.ultimategdbot.api;

import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
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
	 * Gets the discord client representing shard 0.
	 * 
	 * @return the discord client
	 */
	DiscordClient getMainDiscordClient();

	/**
	 * Gets the Flux containing the discord client for each shard.
	 * 
	 * @return a Flux of discord client
	 */
	Flux<DiscordClient> getDiscordClients();

	/**
	 * Gets the database of the bot.
	 * 
	 * @return the database
	 */
	Database getDatabase();
	
	/**
	 * Gets the logger.
	 * 
	 * @return the logger
	 */
	Logger getLogger();

	/**
	 * Gets the channel where the bot sends messages for debugging purposes.
	 * 
	 * @return a Mono emitting the debug log channel
	 */
	Mono<Channel> getDebugLogChannel();

	/**
	 * Gets the channel where the bot can send attachments for its embeds.
	 * 
	 * @return a Mono emitting the attachments channel
	 */
	Mono<Channel> getAttachmentsChannel();

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param message the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(String message);

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param spec the spec of the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(Consumer<MessageCreateSpec> spec);

	/**
	 * Prints a Throwable's stack trace in the log channel. The message may be
	 * splitted in case it doesn't fit in 2000 characters.
	 * 
	 * @param ctx the context in which the error occured
	 * @param t   the throwable to print the strack trace of
	 * @return a Flux emitting all messages sent to logs (if splitted due to
	 *         character limit), or only one message otherwise.
	 */
	Mono<Message> logStackTrace(Context ctx, Throwable t);

	/**
	 * Gets the String representation of an emoji installed on one of the emoji
	 * servers. If the emoji is not found, the returned value is the given name
	 * wrapped in colons.
	 * 
	 * @param emojiName the name of the emoji to look for
	 * @return a Mono emitting the emoji code corresponding to the given name
	 */
	Mono<String> getEmoji(String emojiName);

	/**
	 * Gets the maximum time in seconds that the bot should wait for a reply when a
	 * reply menu is open.
	 * 
	 * @return the value as int (in seconds)
	 */
	int getReplyMenuTimeout();

	/**
	 * Gets the command kernel of this bot.
	 * 
	 * @return the command kernel
	 */
	CommandKernel getCommandKernel();

	/**
	 * Gets a Set containing all successfully loaded plugins.
	 * 
	 * @return a Set of Plugin
	 */
	Set<Plugin> getPlugins();
}
