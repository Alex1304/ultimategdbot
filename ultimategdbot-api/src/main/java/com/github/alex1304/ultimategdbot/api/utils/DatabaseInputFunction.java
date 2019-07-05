package com.github.alex1304.ultimategdbot.api.utils;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Function that determines the value to save to the database after reading the user input as String.
 */
public interface DatabaseInputFunction<D> extends BiFunction<String, Long, Mono<D>> {
	/**
	 * Adds a check on the user input before this function is executed. If the check
	 * is not fulfilled, the Mono returned will emit
	 * {@link IllegalArgumentException} instead of the produced value.
	 * 
	 * @param inputCheck   the predicate to check the input
	 * @param errorMessage the error message that the
	 *                     {@link IllegalArgumentException} should carry
	 * @return a database input function identical to this one if the check passes,
	 *         or turns it into an error otherwise
	 */
	default DatabaseInputFunction<D> withInputCheck(Predicate<String> inputCheck, String errorMessage) {
		return (input, guildId) -> inputCheck.test(input) ? this.apply(input, guildId)
				: Mono.error(new IllegalArgumentException(errorMessage));
	}

	/**
	 * Adds a check on the value produced by this function, after the user input was
	 * process by this function. If the check is not fulfilled, the Mono returned
	 * will emit {@link IllegalArgumentException} instead of the produced value.
	 * 
	 * @param valueCheck   the predicate to check the input
	 * @param errorMessage the error message that the
	 *                     {@link IllegalArgumentException} should carry
	 * @return a database input function identical to this one if the check passes,
	 *         or turns it into an error otherwise
	 */
	default DatabaseInputFunction<D> withValueCheck(Predicate<D> valueCheck, String errorMessage) {
		return (input, guildId) -> this.apply(input, guildId)
				.flatMap(value -> valueCheck.test(value) ? Mono.just(value)
						: Mono.error(new IllegalArgumentException(errorMessage)));
	}

	/**
	 * Forwards the input value to the database as is, without any modification or
	 * check.
	 * 
	 * @return a database input function
	 */
	public static DatabaseInputFunction<String> asIs() {
		return (input, guildId) -> Mono.just(input).filter(x -> !x.equalsIgnoreCase("none"));
	}
	
	/**
	 * Encapsulates a regular BiFunction into an instance of
	 * {@link DatabaseInputFunction}.
	 * 
	 * @param function the function to encapsulate
	 * @return a database input function wrapping the given function
	 */
	public static <D> DatabaseInputFunction<D> of(BiFunction<String, Long, Mono<D>> function) {
		return (input, guildId) -> asIs().apply(input, guildId).flatMap(__ -> function.apply(input, guildId));
	}
	
	/**
	 * Converts the input using the supplied conversion function.
	 * 
	 * @param converter the conversion function
	 * @return a database input function transforming the output to a target type
	 */
	public static <D> DatabaseInputFunction<D> to(Function<? super String, D> converter) {
		return (input, guildId) -> asIs().apply(input, guildId).map(converter::apply);
	}
	
	/**
	 * Converts the input into a role and forwards its ID to the database. If no
	 * role is found, an {@link IllegalArgumentException} is thrown.
	 * 
	 * @param bot the bot instance to use in order to fetch roles
	 * @return a database input function transforming the output to a role ID
	 */
	public static DatabaseInputFunction<Long> toRoleId(Bot bot) {
		return (input, guildId) -> asIs().apply(input, guildId)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(input.substring(3, input.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> bot.getMainDiscordClient().getGuildById(Snowflake.of(guildId))
								.flatMap(g -> g.getRoles()
										.filter(r -> r.getName().equalsIgnoreCase(input))
										.map(Role::getId).next().single()))
				.onErrorMap(e -> new IllegalArgumentException("Could not convert '" + input + "' to a valid role"))
				.map(Snowflake::asLong);
	}
	
	/**
	 * Converts the input into a user and forwards its ID to the database. If no
	 * user is found, an {@link IllegalArgumentException} is thrown.
	 * 
	 * @param bot the bot instance to use in order to fetch users
	 * @return a database input function transforming the output to a user ID
	 */
	public static DatabaseInputFunction<Long> toUserId(Bot bot) {
		return (input, guildId) -> asIs().apply(input, guildId)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(input.substring(2, input.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> Mono.just(input.substring(3, input.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> bot.getMainDiscordClient().getUsers()
						.map(user -> Tuples.of(user, BotUtils.formatDiscordUsername(user)))
						.filter(u -> u.getT2().startsWith(input))
						.map(Tuple2::getT1)
						.map(User::getId)
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException("Could not convert '" + input + "' to a valid user"))
				.map(Snowflake::asLong);
	}
	
	/**
	 * Converts the input into a channel and forwards its ID to the database. If no
	 * channel is found, an {@link IllegalArgumentException} is thrown.
	 * 
	 * @param bot the bot instance to use in order to fetch channels
	 * @param channelType the accepted type of channel 
	 * @return a database input function transforming the output to a channel ID
	 */
	public static DatabaseInputFunction<Long> toChannelId(Bot bot, Class<? extends GuildChannel> channelType) {
		return (input, guildId) -> asIs().apply(input, guildId)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(input.substring(2, input.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> bot.getMainDiscordClient().getGuildById(Snowflake.of(guildId))
								.flatMap(g -> g.getChannels()
										.ofType(channelType)
										.filter(r -> r.getName().equalsIgnoreCase(input))
										.map(Channel::getId).next().single()))
				.onErrorMap(e -> new IllegalArgumentException("Could not convert '" + input + "' to a valid channel"))
				.map(Snowflake::asLong);
	}
}
