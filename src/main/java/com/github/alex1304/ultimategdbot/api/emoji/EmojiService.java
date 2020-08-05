package com.github.alex1304.ultimategdbot.api.emoji;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.BotConfig;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class EmojiService {

	public static final String CONFIG_RESOURCE_NAME = "emoji";
	
	private final GatewayDiscordClient gateway;
	private final Set<Snowflake> emojiGuildIds;

	public EmojiService(BotConfig botConfig, GatewayDiscordClient gateway) {
		this.gateway = gateway;
		this.emojiGuildIds = botConfig.resource(CONFIG_RESOURCE_NAME)
				.readAsStream("emoji_guild_ids", ",")
				.map(Snowflake::of)
				.collect(toUnmodifiableSet());
	}

	public Set<Snowflake> getEmojiGuildIds() {
		return emojiGuildIds;
	}
	
	public Mono<String> get(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		return Flux.fromIterable(emojiGuildIds)
				.flatMap(gateway::getGuildById)
				.flatMap(Guild::getEmojis)
				.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal);
	}
}
