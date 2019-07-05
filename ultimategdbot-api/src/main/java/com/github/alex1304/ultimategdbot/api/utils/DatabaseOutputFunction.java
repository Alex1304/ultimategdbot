package com.github.alex1304.ultimategdbot.api.utils;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Function that determines the value to save to the database after reading the user input as String.
 */
public interface DatabaseOutputFunction<D> extends BiFunction<D, Long, Mono<String>> {
	/**
	 * Forwards the value from the database by returning its string representation
	 * provided by {@link #toString()}
	 * 
	 * @return a database output function
	 */
	public static <D> DatabaseOutputFunction<D> stringValue() {
		return from(Object::toString);
	}
	
	/**
	 * Encapsulates a regular BiFunction into an instance of
	 * {@link DatabaseOutputFunction}.
	 * 
	 * @param function the function to encapsulate
	 * @return a database output function wrapping the given function
	 */
	public static <D> DatabaseOutputFunction<D> of(BiFunction<D, Long, Mono<String>> function) {
		return (value, guildId) -> function.apply(value, guildId);
	}
	
	/**
	 * Forwards the value from the database by returning its string representation
	 * provided by the given function.
	 * 
	 * @param converter the conversion function
	 * @return a database output function providing a string representation for the value
	 */
	public static <D> DatabaseOutputFunction<D> from(Function<? super D, String> converter) {
		return (value, guildId) -> Mono.just(converter.apply(value));
	}
	
	/**
	 * Reads the database value as a role ID, attempts to convert it to a role and
	 * gives a string representation of this role.
	 * 
	 * @param bot the bot instance to use in order to fetch roles
	 * @param roleToString the function that provides the string representation for a role
	 * @return a database output function giving a string representation for the
	 *         role found.
	 */
	public static DatabaseOutputFunction<Long> fromRoleId(Bot bot, Function<? super Role, String> roleToString) {
		return (roleId, guildId) -> bot.getMainDiscordClient()
				.getRoleById(Snowflake.of(guildId), Snowflake.of(roleId))
				.map(roleToString::apply)
				.onErrorResume(e -> Mono.empty());
	}
	
	/**
	 * Reads the database value as a user ID, attempts to convert it to a user and
	 * gives a string representation of this user.
	 * 
	 * @param bot the bot instance to use in order to fetch users
	 * @param userToString the function that provides the string representation for a user
	 * @return a database output function giving a string representation for the
	 *         user found.
	 */
	public static DatabaseOutputFunction<Long> fromUserId(Bot bot, Function<? super User, String> userToString) {
		return (userId, guildId) -> bot.getMainDiscordClient()
				.getUserById(Snowflake.of(userId))
				.map(userToString)
				.onErrorResume(e -> Mono.empty());
	}
	
	/**
	 * Reads the database value as a channel ID, attempts to convert it to a channel and
	 * gives a string representation of this channel.
	 * 
	 * @param bot the bot instance to use in order to fetch channels
	 * @param channelToString the function that provides the string representation for a channel
	 * @return a database output function giving a string representation for the
	 *         channel found.
	 */
	public static DatabaseOutputFunction<Long> fromChannelId(Bot bot, Function<? super GuildChannel, String> channelToString) {
		return (channelId, guildId) -> bot.getMainDiscordClient()
				.getChannelById(Snowflake.of(channelId))
				.ofType(GuildChannel.class)
				.map(channelToString::apply)
				.onErrorResume(e -> Mono.empty());
	}
}
