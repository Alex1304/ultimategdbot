package com.github.alex1304.ultimategdbot.api.emoji;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.service.Service;

import discord4j.rest.util.Snowflake;

public class EmojiService implements Service {

	private final Set<Snowflake> emojiGuildIds;

	EmojiService(Set<Snowflake> emojiGuildIds) {
		this.emojiGuildIds = emojiGuildIds;
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
