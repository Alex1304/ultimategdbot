package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;

/**
 * Contains methods to handle Discord Events
 *
 * @author Alex1304
 */
public class DiscordEvents {
	
	private boolean isReady = false;

	@EventSubscriber
	public void onReady(ReadyEvent event) {
		try {
			Main.resolveContextProps();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		BotUtils.log("Bot started!");
		isReady = true;
	}
	
	@EventSubscriber
	public void onGuildCreated(GuildCreateEvent event) {
		if (!isReady)
			return;
		
		BotUtils.log("New guild joined: " + event.getGuild().getName() + " (" + event.getGuild().getLongID() + ")");
	}

}
