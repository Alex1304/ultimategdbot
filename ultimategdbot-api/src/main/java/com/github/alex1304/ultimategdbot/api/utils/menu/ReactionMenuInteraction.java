package com.github.alex1304.ultimategdbot.api.utils.menu;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.MonoProcessor;

public class ReactionMenuInteraction extends MenuInteraction {
	
	private final ReactionToggleEvent event;

	ReactionMenuInteraction(Message menuMessage, MonoProcessor<Void> closeNotifier, ReactionToggleEvent event) {
		super(menuMessage, closeNotifier);
		this.event = event;
	}

	public ReactionToggleEvent getEvent() {
		return event;
	}
}