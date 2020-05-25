package com.github.alex1304.ultimategdbot.api.command;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

public class CommandServiceFactory implements ServiceFactory<CommandService> {

	@Override
	public CommandService create(Bot bot) {
		return new CommandService(bot);
	}

	@Override
	public Class<CommandService> serviceClass() {
		return CommandService.class;
	}

}
