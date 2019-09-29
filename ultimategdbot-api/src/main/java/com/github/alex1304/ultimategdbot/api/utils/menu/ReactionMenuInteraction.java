package com.github.alex1304.ultimategdbot.api.utils.menu;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;

public class ReactionMenuInteraction extends MenuInteraction {
	
	private final ReactionAddEvent event;

	ReactionMenuInteraction(Message menuMessage, ReactionAddEvent event) {
		super(menuMessage);
		this.event = event;
	}

	public ReactionAddEvent getEvent() {
		return event;
	}
}