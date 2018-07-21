package com.github.alex1304.ultimategdbot.modules.guildevents;

import java.util.List;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;

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
	private List<GuildSettings> initialKnownGuilds;
	
	public GuildEventsModule() {
		this.enabled = false;
		this.initialKnownGuilds = DatabaseUtils.query(GuildSettings.class, "from GuildSettings");
	}

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
		
		if (initialKnownGuilds.stream().anyMatch(gs -> gs.getGuildID() == event.getGuild().getLongID()))
			return;
		
		UltimateGDBot.logInfo(":inbox_tray: New guild joined: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
		
		GuildSettings gs = new GuildSettings();
		gs.setGuildID(event.getGuild().getLongID());
		DatabaseUtils.save(gs);
	}
	
	@EventSubscriber
	public void onGuildLeave(GuildLeaveEvent event) {
		if (!enabled)
			return;
		
		UltimateGDBot.logInfo(":outbox_tray: Guild left: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
		
		GuildSettings gs = DatabaseUtils.findByID(GuildSettings.class, event.getGuild().getLongID());
		if (gs != null)
			DatabaseUtils.delete(gs);
		
		initialKnownGuilds.removeIf(x -> x.getGuildID() == event.getGuild().getLongID());
	}
	
	@EventSubscriber
	public void onGuildUnavailable(GuildUnavailableEvent event) {
		if (!enabled)
			return;
		
		UltimateGDBot.logWarning("Guild unavailable: " + event.getGuild().getName()
				+ " (" + event.getGuild().getLongID() + ")");
	}
}
