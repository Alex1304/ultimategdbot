package com.github.alex1304.ultimategdbot.api.emoji;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.Service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildEmoji;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EmojiService implements Service {

	private final Bot bot;
	private final Set<Snowflake> emojiGuildIds;

	EmojiService(Bot bot) {
		this.bot = bot;
		this.emojiGuildIds = bot.config("emoji")
				.readAsStream("emoji_guild_ids", ",")
				.map(Snowflake::of)
				.collect(toUnmodifiableSet());
	}

	@Override
	public String getName() {
		return "emoji";
	}

	public Set<Snowflake> getEmojiGuildIds() {
		return emojiGuildIds;
	}
	
	public Mono<String> emoji(String emojiName) {
		var defaultVal = ":" + emojiName + ":";
		return Flux.fromIterable(emojiGuildIds)
				.flatMap(bot.gateway()::getGuildById)
				.flatMap(Guild::getEmojis)
				.filter(emoji -> emoji.getName().equalsIgnoreCase(emojiName))
				.next()
				.map(GuildEmoji::asFormat)
				.defaultIfEmpty(defaultVal).onErrorReturn(defaultVal);
	}
}
