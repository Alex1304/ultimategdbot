package com.github.alex1304.ultimategdbot.modules.commands.impl.level;

import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.jdash.api.request.GDLevelSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.impl.level.internal.LevelSearchInternalCommand;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows user to search for a level in Geometry Dash
 *
 * @author Alex1304
 */
public class LevelCommand implements Command {
	
	private int page;
	
	public LevelCommand() {
		this.page = 0;
	}
	
	public LevelCommand(int page) {
		this.page = page;
	}

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (args.isEmpty())
			throw new InvalidCommandArgsException("`" + event.getMessage().getContent() + " <name or ID>`, ex `"
					+ event.getMessage().getContent() + " Level Easy`");
		
		try {
			GDComponentList<GDLevelPreview> results = UltimateGDBot.gdClient().fetch(
					new GDLevelSearchHttpRequest(BotUtils.concatCommandArgs(args), page));
			
			CommandsModule.executeCommand(new LevelSearchInternalCommand(args, results, page), event, new ArrayList<>());
			
		} catch (GDAPIException e) {
			throw new GDServersUnavailableException();
		}
	}

}
