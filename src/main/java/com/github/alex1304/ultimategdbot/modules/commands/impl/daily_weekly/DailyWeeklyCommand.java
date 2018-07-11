package com.github.alex1304.ultimategdbot.modules.commands.impl.daily_weekly;

import java.util.List;

import com.github.alex1304.jdash.api.request.GDTimelyLevelHttpRequest;
import com.github.alex1304.jdash.component.GDTimelyLevel;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows user to see Daily level and Weekly demon info
 *
 * @author Alex1304
 */
public class DailyWeeklyCommand implements Command {
	
	private boolean weekly;

	/**
	 * @param weekly - boolean
	 */
	public DailyWeeklyCommand(boolean weekly) {
		this.weekly = weekly;
	}

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		GDTimelyLevel tl = (GDTimelyLevel) UltimateGDBot.cache()
				.readAndWriteIfNotExists("gd.timely." + weekly, () ->
						UltimateGDBot.gdClient().fetch(new GDTimelyLevelHttpRequest(weekly, UltimateGDBot.gdClient())));
				
		if (tl == null)
			throw new CommandFailedException((weekly ? "Weekly demon" : "Daily level") + " is currently unavailable. Try again later.");
		
		BotUtils.sendMessage(event.getChannel(), event.getAuthor().mention()
				+ ", here is the " + (weekly ? "Weekly demon" : "Daily level") 
				+ " of today. Next in " 
				+ BotUtils.formatTimeMillis(tl.getNextTimelyCooldown() * 1000), 
				GDUtils.buildEmbedForGDLevel((weekly ? "Weekly demon" : "Daily level") + "(#" + tl.getTimelyNumber() + ")",
						(weekly ? "https://i.imgur.com/kcsP5SN.png" : "https://i.imgur.com/enpYuB8.png"), tl));
	}

}
