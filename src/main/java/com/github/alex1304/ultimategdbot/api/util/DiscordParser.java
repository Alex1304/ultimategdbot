package com.github.alex1304.ultimategdbot.api.util;

import com.github.alex1304.ultimategdbot.api.Translator;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;

/**
 * Contains utility methods to parse a user input into a Discord entity.
 */
public final class DiscordParser {
	
	private DiscordParser() {
	}
	
	/**
	 * Parses the input into a Discord user. Emits {@link IllegalArgumentException}
	 * if not found.
	 * 
	 * @param tr      the translator to use to translate the error message in case
	 *                of failure
	 * @param gateway the gateway client used to make requests to Discord
	 * @param str     the input
	 * @return a Mono emitting the found user
	 */
	public static Mono<User> parseUser(Translator tr, GatewayDiscordClient gateway, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(userId -> gateway.getUserById(userId).single())
				.onErrorResume(e -> gateway.getUsers()
						.filter(user -> user.getTag().startsWith(str))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("CommonStrings", "user_not_found", str)));
	}
	
	/**
	 * Parses the input into a Discord role. Emits {@link IllegalArgumentException}
	 * if not found.
	 * 
	 * @param tr      the translator to use to translate the error message in case
	 *                of failure
	 * @param gateway the gateway client used to make requests to Discord
	 * @param guildId the ID of the guild the desired role belongs to
	 * @param str     the input
	 * @return a Mono emitting the found role
	 */
	public static Mono<Role> parseRole(Translator tr, GatewayDiscordClient gateway, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(roleId -> gateway.getRoleById(guildId, roleId).single())
				.onErrorResume(e -> gateway
						.getGuildById(guildId)
						.flatMapMany(Guild::getRoles)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("CommonStrings", "user_not_found", str)));
	}
	
	/**
	 * Parses the input into a Discord channel. Emits
	 * {@link IllegalArgumentException} if not found.
	 * 
	 * @param tr      the translator to use to translate the error message in case
	 *                of failure
	 * @param gateway the gateway client used to make requests to Discord
	 * @param guildId the ID of the guild the desired channel belongs to
	 * @param str     the input
	 * @return a Mono emitting the found channel
	 */
	public static Mono<GuildChannel> parseGuildChannel(Translator tr, GatewayDiscordClient gateway, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(channelId -> gateway.getChannelById(channelId).single())
				.ofType(GuildChannel.class)
				.onErrorResume(e -> gateway.getGuildById(guildId)
						.flatMapMany(Guild::getChannels)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("CommonStrings", "channel_not_found", str)));
	}
	
}
