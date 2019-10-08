package com.github.alex1304.ultimategdbot.api.utils.menu;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.MonoProcessor;

abstract class MenuInteraction {
	
	private final Message menuMessage;
	private final MonoProcessor<Void> closeNotifier;

	MenuInteraction(Message menuMessage, MonoProcessor<Void> closeNotifier) {
		this.menuMessage = menuMessage;
		this.closeNotifier = closeNotifier;
	}

	public Message getMenuMessage() {
		return menuMessage;
	}
	
	public void closeMenu() {
		closeNotifier.onComplete();
	}
}