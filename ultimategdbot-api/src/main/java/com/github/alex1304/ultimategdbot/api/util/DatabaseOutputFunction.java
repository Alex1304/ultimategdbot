package com.github.alex1304.ultimategdbot.api.util;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Function that determines the string to display to the user after reading the value from teh database.
 */
public interface DatabaseOutputFunction<D> extends BiFunction<D, Long, Mono<String>> {
	/**
	 * Forwards the value from the database by returning its string representation
	 * provided by {@link Object#toString()}
	 * 
	 * @param <D> the database type to convert from
	 * @return a database output function
	 */
	public static <D> DatabaseOutputFunction<D> stringValue() {
		return from(Object::toString);
	}
	
	/**
	 * Encapsulates a regular BiFunction into an instance of
	 * {@link DatabaseOutputFunction}.
	 * 
	 * @param <D>      the database type to convert from
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
	 * @param <D>       the database type to convert from
	 * @param converter the conversion function
	 * @return a database output function providing a string representation for the
	 *         value
	 */
	public static <D> DatabaseOutputFunction<D> from(Function<? super D, String> converter) {
		return (value, guildId) -> Mono.just(converter.apply(value));
	}
	
	/**
	 * Reads the database value as a role ID, attempts to convert it to a role and
	 * gives a string representation of this role.
	 * 
	 * @param bot          the bot instance to use in order to fetch roles
	 * @param roleToString the function that provides the string representation for
	 *                     a role
	 * @return a database output function giving a string representation for the
	 *         role found.
	 */
	public static DatabaseOutputFunction<Long> fromRoleId(Bot bot, Function<? super Role, String> roleToString) {
		return (roleId, guildId) -> bot.getGateway()
				.getRoleById(Snowflake.of(guildId), Snowflake.of(roleId))
				.map(roleToString::apply)
				.onErrorResume(e -> Mono.empty());
	}
	
	/**
	 * Reads the database value as a role ID, attempts to convert it to a role and
	 * gives a string representation of this role provided by
	 * {@link DiscordFormatter#formatRole(Role)}.
	 * 
	 * @param bot the bot instance to use in order to fetch roles
	 * @return a database output function giving a string representation for the
	 *         role found.
	 */
	public static DatabaseOutputFunction<Long> fromRoleId(Bot bot) {
		return fromRoleId(bot, DiscordFormatter::formatRole);
	}
	
	/**
	 * Reads the database value as a user ID, attempts to convert it to a user and
	 * gives a string representation of this user.
	 * 
	 * @param bot          the bot instance to use in order to fetch users
	 * @param userToString the function that provides the string representation for
	 *                     a user
	 * @return a database output function giving a string representation for the
	 *         user found.
	 */
	public static DatabaseOutputFunction<Long> fromUserId(Bot bot, Function<? super User, String> userToString) {
		return (userId, guildId) -> bot.getGateway()
				.getUserById(Snowflake.of(userId))
				.map(userToString)
				.onErrorResume(e -> Mono.empty());
	}
	
	/**
	 * Reads the database value as a user ID, attempts to convert it to a user and
	 * gives a string representation of this user provided by
	 * {@link DiscordFormatter#formatUser(User)}.
	 * 
	 * @param bot the bot instance to use in order to fetch users
	 * @return a database output function giving a string representation for the
	 *         user found.
	 */
	public static DatabaseOutputFunction<Long> fromUserId(Bot bot) {
		return fromUserId(bot, DiscordFormatter::formatUser);
	}
	
	/**
	 * Reads the database value as a channel ID, attempts to convert it to a channel
	 * and gives a string representation of this channel.
	 * 
	 * @param bot             the bot instance to use in order to fetch channels
	 * @param channelToString the function that provides the string representation
	 *                        for a channel
	 * @return a database output function giving a string representation for the
	 *         channel found.
	 */
	public static DatabaseOutputFunction<Long> fromChannelId(Bot bot, Function<? super GuildChannel, String> channelToString) {
		return (channelId, guildId) -> bot.getGateway()
				.getChannelById(Snowflake.of(channelId))
				.ofType(GuildChannel.class)
				.map(channelToString::apply)
				.onErrorResume(e -> Mono.empty());
	}
	
	/**
	 * Reads the database value as a channel ID, attempts to convert it to a channel
	 * and gives a string representation of this channel provided by
	 * {@link DiscordFormatter#formatGuildChannel(GuildChannel)}.
	 * 
	 * @param bot the bot instance to use in order to fetch channels
	 * @return a database output function giving a string representation for the
	 *         channel found.
	 */
	public static DatabaseOutputFunction<Long> fromChannelId(Bot bot) {
		return fromChannelId(bot, DiscordFormatter::formatGuildChannel);
	}
}
