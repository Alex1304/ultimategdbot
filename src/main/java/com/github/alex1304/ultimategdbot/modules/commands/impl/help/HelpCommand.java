package com.github.alex1304.ultimategdbot.modules.commands.impl.help;

import java.util.List;
import java.util.Map.Entry;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Help command gives details on the command list, tells the user how to use the
 * bot, etc.
 *
 * @author Alex1304
 */
public class HelpCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		try {
			CommandsModule cm = (CommandsModule) UltimateGDBot.getModule("commands");
			
			StringBuffer formattedCmdList = new StringBuffer();
			
			if (args.isEmpty()) {
				formattedCmdList.append(event.getAuthor().mention());
				formattedCmdList.append(", here is the list of commands you can use in this channel:\n\n");
				
				for (Entry<String, Command> cmd : cm.getCommandMap().entrySet()) {
					if (BotRoles.isGrantedAll(event.getAuthor(), event.getChannel(), cmd.getValue().getRolesRequired())) {
						formattedCmdList.append("`");
						formattedCmdList.append(cmd.getKey());
						formattedCmdList.append("`, ");
					}
				}
				
				formattedCmdList.delete(formattedCmdList.length() - 2, formattedCmdList.length());
			}
			
			formattedCmdList.append("\n\n__Available prefixes:__\n");
			formattedCmdList.append(UltimateGDBot.property("ultimategdbot.prefix.canonical"));
			formattedCmdList.append("*command*\n");
			formattedCmdList.append(UltimateGDBot.property("ultimategdbot.prefix.full"));
			formattedCmdList.append("*command*\n");
			formattedCmdList.append(UltimateGDBot.client().getOurUser().mention());
			formattedCmdList.append(" *command*\n");
			
			formattedCmdList.append("\n**To get help on a specific command, check out the user manual:** http://ultimategdbot.readthedocs.io/en/latest/Commands-documentation/");
			
			BotUtils.sendMessage(event.getChannel(), formattedCmdList.toString());
		} catch (ModuleUnavailableException e) {
			throw new CommandFailedException("Commands are temporarily unavailable");
}
	}
	
}
