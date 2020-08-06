package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import java.util.Optional;
import java.util.function.Function;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;

/**
 * Utility class that provides factories for guild configuration value getters.
 * It allows the use of method references on data objects, by hiding the fact
 * that a value is asynchronous.
 */
public final class ValueGetters {

	private ValueGetters() {
		throw new AssertionError();
	}
	
	/**
	 * Convenient method to generate a value getter for simple value-based
	 * configuration entries.
	 * 
	 * @param <D>    the type of data object
	 * @param <T>    the type of value to get
	 * @param getter a Function that gets the value from the data object, typically
	 *               a method reference
	 * @return a value getter for a simple value
	 */
	public static <D extends GuildConfigData<D>, T> Function<D, Mono<T>> forSimpleValue(Function<? super D, ? extends T> getter) {
		return data -> Mono.justOrEmpty(getter.apply(data));
	}

	/**
	 * Convenient method to generate a value getter for optional value-based
	 * configuration entries.
	 * 
	 * @param <D>    the type of data object
	 * @param <T>    the type of value to get
	 * @param getter a Function that gets the optional value from the data object,
	 *               typically a method reference
	 * @return a value getter for an optional value
	 */
	public static <D extends GuildConfigData<D>, T> Function<D, Mono<T>> forOptionalValue(Function<? super D, Optional<? extends T>> getter) {
		return data -> Mono.justOrEmpty(getter.apply(data));
	}

	/**
	 * Convenient method to generate a value getter for guild channel configuration
	 * entries.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the channel
	 * @param idGetter a Function that gets the channel ID from the data object
	 * @return a value getter for a guild channel
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<GuildChannel>> forGuildChannel(GatewayDiscordClient gateway,
			Function<? super D, Snowflake> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(gateway::getChannelById)
				.ofType(GuildChannel.class)
				.filter(channel -> channel.getGuildId().equals(data.guildId()));
	}

	/**
	 * Convenient method to generate a value getter for guild role configuration
	 * entries.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the role
	 * @param idGetter a Function that gets the role ID from the data object
	 * @return a value getter for a guild role
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<Role>> forGuildRole(GatewayDiscordClient gateway,
			Function<? super D, Snowflake> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(roleId -> gateway.getRoleById(data.guildId(), roleId))
				.filter(role -> role.getGuildId().equals(data.guildId()));
	}

	/**
	 * Convenient method to generate a value getter for guild member configuration
	 * entries.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the member
	 * @param idGetter a Function that gets the member ID from the data object
	 * @return a value getter for a guild member
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<Member>> forGuildMember(GatewayDiscordClient gateway,
			Function<? super D, Snowflake> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(memberId -> gateway.getMemberById(data.guildId(), memberId))
				.filter(member -> member.getGuildId().equals(data.guildId()));
	}

	/**
	 * Convenient method to generate a value getter for guild channel configuration
	 * entries. This handles the case where the channel is optional.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the channel
	 * @param idGetter a Function that gets the channel ID from the data object
	 * @return a value getter for a guild channel
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<GuildChannel>> forOptionalGuildChannel(GatewayDiscordClient gateway,
			Function<? super D, Optional<Snowflake>> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(gateway::getChannelById)
				.ofType(GuildChannel.class)
				.filter(channel -> channel.getGuildId().equals(data.guildId()));
	}

	/**
	 * Convenient method to generate a value getter for guild role configuration
	 * entries. This handles the case where the role is optional.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the role
	 * @param idGetter a Function that gets the role ID from the data object
	 * @return a value getter for a guild role
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<Role>> forOptionalGuildRole(GatewayDiscordClient gateway,
			Function<? super D, Optional<Snowflake>> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(roleId -> gateway.getRoleById(data.guildId(), roleId))
				.filter(role -> role.getGuildId().equals(data.guildId()));
	}

	/**
	 * Convenient method to generate a value getter for guild member configuration
	 * entries. This handles the case where the member is optional.
	 * 
	 * @param <D>      the type of data object
	 * @param gateway  the gateway client to use to retrieve the member
	 * @param idGetter a Function that gets the member ID from the data object
	 * @return a value getter for a guild member
	 */
	public static <D extends GuildConfigData<D>> Function<D, Mono<Member>> forOptionalGuildMember(GatewayDiscordClient gateway,
			Function<? super D, Optional<Snowflake>> idGetter) {
		return data -> Mono.justOrEmpty(idGetter.apply(data))
				.flatMap(memberId -> gateway.getMemberById(data.guildId(), memberId))
				.filter(member -> member.getGuildId().equals(data.guildId()));
	}
}
