package com.github.alex1304.ultimategdbot.api.localization;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

public class LocalizationServiceFactory implements ServiceFactory<LocalizationService> {

	@Override
	public LocalizationService create(Bot bot) {
		return new LocalizationService(bot);
	}

	@Override
	public Class<LocalizationService> serviceClass() {
		return LocalizationService.class;
	}

}
