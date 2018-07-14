package com.github.alex1304.ultimategdbot.modules.guildevents;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.modules.Module;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.GuildUnavailableEvent;

/**
 * Logs events happening with guilds
 *
 * @author Alex1304
 */
public class GuildEventsModule implements Module {

	private boolean enabled;

	@Override
	public void start() {
		enabled = true;
	}

	@Override
	public void stop() {
		enabled = false;
	}
	
	@EventSubscriber
	public void onGuildCreate(GuildCreateEvent event) {
		if (!enabled)
			return;
		
		UltimateGDBot.logSuccess("New guild joined: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
	}
	
	@EventSubscriber
	public void onGuildLeave(GuildLeaveEvent event) {
		if (!enabled)
			return;
		
		UltimateGDBot.logError("Guild left: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
	}
	
	@EventSubscriber
	public void onGuildUnavailable(GuildUnavailableEvent event) {
		if (!enabled)
			return;
		
		UltimateGDBot.logWarning("Guild unavailable: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
	}
}
