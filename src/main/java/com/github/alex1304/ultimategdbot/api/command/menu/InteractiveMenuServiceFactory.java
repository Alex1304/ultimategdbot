package com.github.alex1304.ultimategdbot.api.command.menu;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.command.CommandServiceFactory;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiServiceFactory;
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

	@Override
	public Set<ServiceFactory<?>> dependedServices() {
		return Set.of(new CommandServiceFactory(), new EmojiServiceFactory());
	}
}
