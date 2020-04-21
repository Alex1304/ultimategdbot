package com.github.alex1304.ultimategdbot.api.util.menu;

import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.api.util.menu.InteractiveMenu.MenuTermination;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.MonoProcessor;

public class ReactionMenuInteraction extends MenuInteraction {
	
	private final ReactionToggleEvent event;

	ReactionMenuInteraction(Message menuMessage, ConcurrentHashMap<String, Object> contextVariables, MonoProcessor<MenuTermination> closeNotifier,
			ReactionToggleEvent event) {
		super(menuMessage, contextVariables, closeNotifier);
		this.event = event;
	}

	public ReactionToggleEvent getEvent() {
		return event;
	}
}