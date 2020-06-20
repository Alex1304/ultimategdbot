package com.github.alex1304.ultimategdbot.api.util;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Translator;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

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
	 * @param tr  the translator to use to translate the error message in case of
	 *            failure
	 * @param bot the bot used to make requests to Discord
	 * @param str the input
	 * @return a Mono emitting the found user
	 */
	public static Mono<User> parseUser(Translator tr, Bot bot, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(userId -> bot.gateway().getUserById(userId).single())
				.onErrorResume(e -> bot.gateway().getUsers()
						.filter(user -> user.getTag().startsWith(str))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("strings.common", "user_not_found", str)));
	}
	
	/**
	 * Parses the input into a Discord role. Emits {@link IllegalArgumentException}
	 * if not found.
	 * 
	 * @param tr      the translator to use to translate the error message in case
	 *                of failure
	 * @param bot     the bot used to make requests to Discord
	 * @param guildId the ID of the guild the desired role belongs to
	 * @param str     the input
	 * @return a Mono emitting the found role
	 */
	public static Mono<Role> parseRole(Translator tr, Bot bot, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(roleId -> bot.gateway().getRoleById(guildId, roleId).single())
				.onErrorResume(e -> bot.gateway()
						.getGuildById(guildId)
						.flatMapMany(Guild::getRoles)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("strings.common", "user_not_found", str)));
	}
	
	/**
	 * Parses the input into a Discord channel. Emits
	 * {@link IllegalArgumentException} if not found.
	 * 
	 * @param tr      the translator to use to translate the error message in case
	 *                of failure
	 * @param bot     the bot used to make requests to Discord
	 * @param guildId the ID of the guild the desired channel belongs to
	 * @param str     the input
	 * @return a Mono emitting the found channel
	 */
	public static Mono<GuildChannel> parseGuildChannel(Translator tr, Bot bot, Snowflake guildId, String str) {
		return Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.flatMap(channelId -> bot.gateway().getChannelById(channelId).single())
				.ofType(GuildChannel.class)
				.onErrorResume(e -> bot.gateway().getGuildById(guildId)
						.flatMapMany(Guild::getChannels)
						.filter(r -> r.getName().toLowerCase().startsWith(str.toLowerCase()))
						.next()
						.single())
				.onErrorMap(e -> new IllegalArgumentException(tr.translate("strings.common", "channel_not_found", str)));
	}
	
}
