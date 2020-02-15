package com.github.alex1304.ultimategdbot.api.util;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Contains utility methods to parse a user input into a Discord entity.
 */
public class DiscordParser {
	
	private DiscordParser() {
	}
	
	/**
	 * Parses the input into a Discord user. Emits {@link IllegalArgumentException}
	 * if not found.
	 * 
	 * @param bot the bot used to make requests to Discord
	 * @param str the input
	 * @return a Mono emitting the found user
	 */
	public static Mono<User> parseUser(Bot bot, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(bot.getGateway()::getUserById)
				.onErrorResume(e -> bot.getGateway().getUsers()
						.map(user -> Tuples.of(user, DiscordFormatter.formatUser(user)))
						.filter(u -> u.getT2().startsWith(str))
						.map(Tuple2::getT1)
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException("Cannot find user '" + str + "'."));
	}
	
	/**
	 * Parses the input into a Discord role. Emits {@link IllegalArgumentException}
	 * if not found.
	 * 
	 * @param bot     the bot used to make requests to Discord
	 * @param guildId the ID of the guild the desired role belongs to
	 * @param str     the input
	 * @return a Mono emitting the found role
	 */
	public static Mono<Role> parseRole(Bot bot, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(roleId -> bot.getGateway().getRoleById(guildId, roleId))
				.onErrorResume(e -> bot.getGateway()
						.getGuildById(guildId)
						.flatMapMany(Guild::getRoles)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException("Cannot find role '" + str + "'."));
	}
	
	/**
	 * Parses the input into a Discord channel. Emits
	 * {@link IllegalArgumentException} if not found.
	 * 
	 * @param bot     the bot used to make requests to Discord
	 * @param guildId the ID of the guild the desired channel belongs to
	 * @param str     the input
	 * @return a Mono emitting the found channel
	 */
	public static Mono<GuildChannel> parseGuildChannel(Bot bot, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(bot.getGateway()::getChannelById)
				.ofType(GuildChannel.class)
				.onErrorResume(e -> bot.getGateway().getGuildById(guildId)
						.flatMapMany(Guild::getChannels)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException("Cannot find channel '" + str + "'."));
	}
	
}
