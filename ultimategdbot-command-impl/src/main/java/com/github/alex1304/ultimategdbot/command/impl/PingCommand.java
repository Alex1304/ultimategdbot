package com.github.alex1304.ultimategdbot.command.impl;

import com.github.alex1304.ultimategdbot.command.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
import com.github.alex1304.ultimategdbot.command.api.DiscordContext;
import com.github.alex1304.ultimategdbot.command.api.DiscordView;

public class PingCommand implements DiscordCommand {

	@Override
	public DiscordView execute(DiscordContext ctx) throws CommandFailedException {
		return new DiscordView(":ping_pong: Pong!");
	}

}
