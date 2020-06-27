package com.github.alex1304.ultimategdbot.api.command.menu;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

import reactor.core.publisher.Mono;

public class InteractiveMenuServiceFactory implements ServiceFactory<InteractiveMenuService> {

	@Override
	public Mono<InteractiveMenuService> create(Bot bot) {
		return Mono.fromCallable(() -> new InteractiveMenuService(bot));
	}

	@Override
	public Class<InteractiveMenuService> serviceClass() {
		return InteractiveMenuService.class;
	}
}
