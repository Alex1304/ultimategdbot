package com.github.alex1304.ultimategdbot.api.localization;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

import reactor.core.publisher.Mono;

public class LocalizationServiceFactory implements ServiceFactory<LocalizationService> {

	@Override
	public Mono<LocalizationService> create(Bot bot) {
		return Mono.fromCallable(() -> new LocalizationService(bot));
	}

	@Override
	public Class<LocalizationService> serviceClass() {
		return LocalizationService.class;
	}

}
