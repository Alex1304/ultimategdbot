package com.github.alex1304.ultimategdbot.command.impl;

import com.github.alex1304.ultimategdbot.command.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
import com.github.alex1304.ultimategdbot.command.api.DiscordContext;
import com.github.alex1304.ultimategdbot.logic.Utils;

import discord4j.core.spec.MessageCreateSpec;

public class PongCommand implements DiscordCommand {

	@Override
	public MessageCreateSpec execute(DiscordContext ctx) throws CommandFailedException {
		return Utils.messageOf(":ping_pong: Pong!");
	}

}
