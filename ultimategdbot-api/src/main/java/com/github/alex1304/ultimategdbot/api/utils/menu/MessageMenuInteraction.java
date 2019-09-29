package com.github.alex1304.ultimategdbot.api.utils.menu;

import com.github.alex1304.ultimategdbot.api.command.ArgumentList;
import com.github.alex1304.ultimategdbot.api.command.FlagSet;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

public class MessageMenuInteraction extends MenuInteraction {
	
	private final MessageCreateEvent event;
	private final ArgumentList args;
	private final FlagSet flags;

	MessageMenuInteraction(Message menuMessage, MessageCreateEvent event, ArgumentList args, FlagSet flags) {
		super(menuMessage);
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