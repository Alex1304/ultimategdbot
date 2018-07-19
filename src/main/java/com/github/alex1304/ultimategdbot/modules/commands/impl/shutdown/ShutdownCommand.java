package com.github.alex1304.ultimategdbot.modules.commands.impl.shutdown;

import java.util.EnumSet;
import java.util.List;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows bot owner to shut the bot down
 *
 * @author Alex1304
 */
public class ShutdownCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		BotUtils.sendMessage(event.getChannel(), "Disconnecting...");
		UltimateGDBot.logInfo("Disconnecting...");
		System.exit(0);
	}

	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.OWNER);
	}
}
