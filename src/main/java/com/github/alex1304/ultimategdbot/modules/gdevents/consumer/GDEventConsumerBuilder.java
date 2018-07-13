package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.component.GDComponent;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

/**
 * Builds a GD event consumer that will broadcast info on the event to all subscribed guilds
 * 
 * @param <T> - type of component subject to the GD event
 *
 * @author Alex1304
 */
public abstract class GDEventConsumerBuilder<T extends GDComponent> {
	
	private String dbChannelField;
	protected String eventName;
	protected BroadcastableMessage messageToBroadcast;

	public GDEventConsumerBuilder(String eventName, String dbChannelField, BroadcastableMessage messageToBroadcast) {
		this.eventName = eventName;
		this.dbChannelField = dbChannelField;
		this.messageToBroadcast = messageToBroadcast;
	}
	
	public Consumer<T> build() {
		return component -> {
			long beginMillis = System.currentTimeMillis();
			UltimateGDBot.logInfo("GD event fired: **" + eventName + "** for " + componentToHumanReadableString(component));
			
			this.executeBefore(component);
			
			List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g." + dbChannelField + " > 0");
			
			Map<Long, GuildSettings> channelToGS = new ConcurrentHashMap<>();
			
			List<IChannel> channels = gsList.parallelStream()
					.filter(gs -> {
						Optional<IGuild> og = UltimateGDBot.client().getGuilds().parallelStream()
								.filter(g -> g.getLongID() == gs.getGuildID())
								.findAny();
						if (og.isPresent()) {
							gs.setGuildInstance(og.get());
							return true;
						} else
							return false;
					})
					.map(gs -> {
						IChannel c = new ChannelAwardedLevelsSetting(gs).getValue();
						
						if (c != null)
							channelToGS.put(c.getLongID(), gs);
						return c;
					})
					.filter(c -> c != null)
					.collect(Collectors.toList());
			
			long prepTime = System.currentTimeMillis() - beginMillis;
			
			this.broadcastComponent(component, channels, channelToGS);
			
			long broadcastTime = System.currentTimeMillis() - beginMillis - prepTime;
			
			this.executeAfter(component);
			
			UltimateGDBot.logSuccess("Successfully processed GD event **" + eventName + "** for " 
					+ componentToHumanReadableString(component) + ".\n"
					+ "Gathered info on subscribed guilds in: " + BotUtils.formatTimeMillis(prepTime) + "\n"
					+ "Broadcast message to " + channels.size() + " guilds in: " + BotUtils.formatTimeMillis(broadcastTime) + "\n"
					+ "**Total execution time: " + BotUtils.formatTimeMillis(prepTime + broadcastTime) + "**");
			
		};
	}
	
	/**
	 * Do operations with the component before everything start
	 * 
	 * @param component - T
	 */
	protected void executeBefore(T component) {}
	
	/**
	 * Do the main operations with the component after guild info has been loaded
	 * 
	 * @param component - T
	 * @param channels - List&lt;IChannel&gt;
	 * @param channelToGS - Map&lt;Long, GuildSettings&gt;
	 */
	protected abstract void broadcastComponent(T component, List<IChannel> channels, Map<Long, GuildSettings> channelToGS);

	/**
	 * Do operations with the component after it has been broadcast
	 * 
	 * @param component - T
	 */
	protected void executeAfter(T component) {}
	
	/**
	 * Builds a human readable string representing the component
	 * 
	 * @param component - T
	 * @return String
	 */
	protected abstract String componentToHumanReadableString(T component);
}
