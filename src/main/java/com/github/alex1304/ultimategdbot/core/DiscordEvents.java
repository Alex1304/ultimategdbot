package com.github.alex1304.ultimategdbot.core;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.RequestBuffer;

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
			RequestBuffer.request(() -> {
				UltimateGDBot.client().changePresence(StatusType.IDLE, ActivityType.PLAYING, "No commands available");
			});
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		UltimateGDBot.logInfo("Bot started!");
		isReady = true;
	}
	
	@EventSubscriber
	public void onGuildCreated(GuildCreateEvent event) {
		if (!isReady)
			return;
		
		UltimateGDBot.logInfo("New guild joined: " + event.getGuild().getName() + " (" + event.getGuild().getLongID() + ")");
	}
	
	@EventSubscriber
	public void onGuildLeft(GuildLeaveEvent event) {
		if (!isReady)
			return;
		
		UltimateGDBot.logInfo("Guild left: " + event.getGuild().getName() + " (" + event.getGuild().getLongID() + ")");
	}

}
