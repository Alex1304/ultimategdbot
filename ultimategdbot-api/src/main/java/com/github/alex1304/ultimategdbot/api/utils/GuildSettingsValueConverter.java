package com.github.alex1304.ultimategdbot.api.utils;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class GuildSettingsValueConverter {
	public static final String NONE_VALUE = "None";
	private final Bot bot;
	
	public GuildSettingsValueConverter(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
	}
	
	public Mono<String> justCheck(String str, long guildId, Predicate<String> check, String errorMessage) {
		return Mono.just(str)
				.filter(check)
				.switchIfEmpty(Mono.error(new IllegalArgumentException(errorMessage)));
	}
	
	public Mono<String> noConversion(String str, long guildId) {
		return Mono.just(str);
	}
	
	public <N extends Number> Mono<N> toNumber(String str, long guildId, Function<String, N> converter) {
		return Mono.fromCallable(() -> converter.apply(str));
	}
	
	public <N extends Number> Mono<N> toNumberWithCheck(String str, long guildId, Function<String, N> converter, Predicate<N> check, String errorMessage) {
		return toNumber(str, guildId, converter)
				.filter(check)
				.switchIfEmpty(Mono.error(new IllegalArgumentException(errorMessage)));
	}
	
	public Mono<String> fromNumber(Number num, long guildId) {
		return Mono.just(num).map(String::valueOf);
	}
	
	public Mono<Boolean> toBoolean(String str, long guildId) {
		return Mono.just(str).filter(x -> x.matches("yes|true|1|enable")).map(__ -> true)
				.switchIfEmpty(Mono.just(str).filter(x -> x.matches("no|false|0|disable")).map(__ -> false))
				.switchIfEmpty(Mono.error(new IllegalArgumentException("Expected a boolean value (yes/no, true/false, "
						+ "1/0 or enable/disable). Note that it's case sensitive.")));
	}
	
	public Mono<Long> toRoleId(String str, long guildId) {
		return str.equalsIgnoreCase(NONE_VALUE) ? Mono.just(0L) : Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(3, str.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> bot.getDiscordClients().next()
						.flatMap(client -> client.getGuildById(Snowflake.of(guildId)))
								.flatMap(g -> g.getRoles()
										.filter(r -> r.getName().equalsIgnoreCase(str))
										.map(Role::getId).next()))
				.onErrorResume(e -> Mono.empty())
				.switchIfEmpty(Mono.error(new IllegalArgumentException("Could not convert '" + str + "' to a valid role")))
				.map(Snowflake::asLong);
	}
	
	public Mono<String> fromRoleId(long roleId, long guildId) {
		return roleId == 0 ? Mono.just(NONE_VALUE) : bot.getDiscordClients().next()
				.flatMap(client -> client.getRoleById(Snowflake.of(guildId), Snowflake.of(roleId)))
				.map(role -> '@' + role.getName() + " (" + role.getId().asLong() + ")")
				.onErrorResume(e -> Mono.empty())
				.defaultIfEmpty("_(unknown role of ID " + roleId + ")_");
	}
	
	private Mono<Long> toChannelId(String str, long guildId, Type channelType) {
		return str.equalsIgnoreCase(NONE_VALUE) ? Mono.just(0L) : Mono.just(str)
				.map(Snowflake::of)
				.onErrorResume(e -> Mono.just(str.substring(2, str.length() - 1))
						.map(Snowflake::of))
				.onErrorResume(e -> bot.getDiscordClients().next()
						.flatMap(client -> client.getGuildById(Snowflake.of(guildId)))
								.flatMap(g -> g.getChannels()
										.filter(c -> c.getType() == channelType)
										.filter(c -> c.getName().equalsIgnoreCase(str))
										.map(Channel::getId)
										.next()))
				.onErrorResume(e -> Mono.empty())
				.switchIfEmpty(Mono.error(new IllegalArgumentException("Could not convert '" + str + "' to a valid channel")))
				.map(Snowflake::asLong);
	}
	
	public Mono<Long> toTextChannelId(String str, long guildId) {
		return toChannelId(str, guildId, Type.GUILD_TEXT);
	}
	
	public Mono<Long> toVoiceChannelId(String str, long guildId) {
		return toChannelId(str, guildId, Type.GUILD_VOICE);
	}
	
	public Mono<Long> toCategoryId(String str, long guildId) {
		return toChannelId(str, guildId, Type.GUILD_CATEGORY);
	}
	
	public Mono<String> fromChannelId(long channelId, long guildId) {
		return channelId == 0 ? Mono.just(NONE_VALUE) : bot.getDiscordClients().next()
				.flatMap(client -> client.getChannelById(Snowflake.of(channelId)))
				.ofType(GuildChannel.class)
				.map(channel -> channel.getType() == Type.GUILD_TEXT ? channel.getMention() : channel.getName() + " (" + channel.getId().asLong() + ")")
				.onErrorResume(e -> Mono.empty())
				.defaultIfEmpty("_(unknown channel of ID " + channelId + ")_");
	}
	
}
