package com.github.alex1304.ultimategdbot.api.emoji;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

import reactor.core.publisher.Mono;

public class EmojiServiceFactory implements ServiceFactory<EmojiService> {

	@Override
	public Mono<EmojiService> create(Bot bot) {
		return Mono.fromCallable(() -> new EmojiService(bot));
	}

	@Override
	public Class<EmojiService> serviceClass() {
		return EmojiService.class;
	}

}
