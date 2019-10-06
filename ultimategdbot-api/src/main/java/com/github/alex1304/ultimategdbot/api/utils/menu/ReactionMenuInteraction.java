package com.github.alex1304.ultimategdbot.api.utils.menu;

import discord4j.core.object.entity.Message;

public class ReactionMenuInteraction extends MenuInteraction {
	
	private final ReactionToggleEvent event;

	ReactionMenuInteraction(Message menuMessage, ReactionToggleEvent event) {
		super(menuMessage);
		this.event = event;
	}

	public ReactionToggleEvent getEvent() {
		return event;
	}
}