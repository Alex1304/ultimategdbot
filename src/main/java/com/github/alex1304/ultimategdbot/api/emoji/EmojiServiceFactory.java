package com.github.alex1304.ultimategdbot.api.emoji;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;

public class EmojiServiceFactory implements ServiceFactory<EmojiService> {

	@Override
	public EmojiService create(Bot bot) {
		return new EmojiService(bot);
	}

	@Override
	public Class<EmojiService> serviceClass() {
		return EmojiService.class;
	}

}
