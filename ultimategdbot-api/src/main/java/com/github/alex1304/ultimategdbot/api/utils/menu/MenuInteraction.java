package com.github.alex1304.ultimategdbot.api.utils.menu;

import discord4j.core.object.entity.Message;

abstract class MenuInteraction {
	
	private final Message menuMessage;

	MenuInteraction(Message menuMessage) {
		this.menuMessage = menuMessage;
	}

	public Message getMenuMessage() {
		return menuMessage;
	}
}