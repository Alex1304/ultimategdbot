package com.github.alex1304.ultimategdbot.api.command;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

import reactor.core.publisher.Mono;

public class CommandServiceFactory implements ServiceFactory<CommandService> {

	@Override
	public Mono<CommandService> create(Bot bot) {
		return Mono.fromCallable(() -> new CommandService(bot));
	}

	@Override
	public Class<CommandService> serviceClass() {
		return CommandService.class;
	}

}
