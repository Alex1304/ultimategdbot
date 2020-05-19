package com.github.alex1304.ultimategdbot.api.emoji;

import static java.util.stream.Collectors.toUnmodifiableSet;

import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

public class EmojiServiceFactory implements ServiceFactory<EmojiService> {

	@Override
	public Mono<EmojiService> create(PropertyReader properties) {
		return Mono.fromSupplier(() -> new EmojiService(
				properties.readAsStream("emoji_guild_ids", ",").map(Snowflake::of).collect(toUnmodifiableSet())));
	}

	@Override
	public Class<EmojiService> type() {
		return EmojiService.class;
	}

}
