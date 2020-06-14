package com.github.alex1304.ultimategdbot.api.emoji;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.Service;

import discord4j.common.util.Snowflake;

public class EmojiService implements Service {

	private final Set<Snowflake> emojiGuildIds;

	EmojiService(Bot bot) {
		this.emojiGuildIds = bot.config()
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
	
	public String emoji(String name) {
		return ""; // TODO
	}
}
