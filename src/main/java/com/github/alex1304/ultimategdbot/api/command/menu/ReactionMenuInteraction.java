package com.github.alex1304.ultimategdbot.api.command.menu;

import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.api.Translator;
import com.github.alex1304.ultimategdbot.api.command.menu.InteractiveMenu.MenuTermination;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.MonoProcessor;

public final class ReactionMenuInteraction extends MenuInteraction {
	
	private final ReactionToggleEvent event;

	ReactionMenuInteraction(Translator tr, Message menuMessage, ConcurrentHashMap<String, Object> contextVariables, MonoProcessor<MenuTermination> closeNotifier,
			ReactionToggleEvent event) {
		super(tr, menuMessage, contextVariables, closeNotifier);
		this.event = event;
	}

	public ReactionToggleEvent getEvent() {
		return event;
	}
}