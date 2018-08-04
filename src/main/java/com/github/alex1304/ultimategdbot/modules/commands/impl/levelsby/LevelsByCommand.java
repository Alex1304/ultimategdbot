package com.github.alex1304.ultimategdbot.modules.commands.impl.levelsby;

import java.util.List;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.UnknownUserException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.GDLevelSearchBuilder;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows to search for levels by user
 *
 * @author Alex1304
 */
public class LevelsByCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		GDUser user = GDUtils.guessGDUserFromString(BotUtils.concatCommandArgs(args));
		
		if (user == null)
			throw new UnknownUserException();
		GDLevelSearchBuilder lsb = new GDLevelSearchBuilder();
		
		lsb.setType(5);
		lsb.setKeywords(user.getPlayerID() + "");
		
		GDComponentList<GDLevelPreview> results = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
				.readAndWriteIfNotExists("gd.levelsearch.byuser." + user.getPlayerID(), 
						() -> UltimateGDBot.gdClient().fetch(lsb.build()));
		
		System.out.println(results);
	}

}
