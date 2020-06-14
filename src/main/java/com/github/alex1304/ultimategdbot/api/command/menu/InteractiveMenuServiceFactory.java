package com.github.alex1304.ultimategdbot.api.command.menu;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

public class InteractiveMenuServiceFactory implements ServiceFactory<InteractiveMenuService> {

	@Override
	public InteractiveMenuService create(Bot bot) {
		return new InteractiveMenuService(bot);
	}

	@Override
	public Class<InteractiveMenuService> serviceClass() {
		return InteractiveMenuService.class;
	}
}
