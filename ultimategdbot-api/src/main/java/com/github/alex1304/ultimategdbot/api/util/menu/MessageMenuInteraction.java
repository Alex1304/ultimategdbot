package com.github.alex1304.ultimategdbot.api.util.menu;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.FlagSet;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.MonoProcessor;

public class MessageMenuInteraction extends MenuInteraction {
	
	private final MessageCreateEvent event;
	private final ArgumentList args;
	private final FlagSet flags;

	MessageMenuInteraction(Message menuMessage, MonoProcessor<Void> closeNotifier,
			MessageCreateEvent event, ArgumentList args, FlagSet flags) {
		super(menuMessage, closeNotifier);
		this.event = event;
		this.args = args;
		this.flags = flags;
	}

	public MessageCreateEvent getEvent() {
		return event;
	}

	public ArgumentList getArgs() {
		return args;
	}

	public FlagSet getFlags() {
		return flags;
	}
}