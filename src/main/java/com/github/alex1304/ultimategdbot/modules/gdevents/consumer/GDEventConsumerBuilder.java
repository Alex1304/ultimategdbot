package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.component.GDComponent;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import javafx.collections.ObservableList;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

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
	protected Supplier<BroadcastableMessage> messageToBroadcast;
	protected Function<GuildSettings, IChannel> broadcastChannel;
	protected Procedure onBroadcastDone;
	protected Consumer<ObservableList<? extends IMessage>> onDone;

	public GDEventConsumerBuilder(String eventName, String dbChannelField, Supplier<BroadcastableMessage> messageToBroadcast, Function<GuildSettings, IChannel> broadcastChannel) {
		this.eventName = eventName;
		this.dbChannelField = dbChannelField;
		this.messageToBroadcast = messageToBroadcast;
		this.broadcastChannel = broadcastChannel;
		this.onBroadcastDone = () -> {};
		this.onDone = results -> {};
	}
	
	public Consumer<T> build() {
		return component -> {
			long beginMillis = System.currentTimeMillis();
			UltimateGDBot.logInfo("GD event fired: **" + eventName + "** for " + componentToHumanReadableString(component));
			
			this.executeBefore(component);
			
			List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g." + dbChannelField + " > 0");
			
			Map<Long, GuildSettings> channelToGS = new ConcurrentHashMap<>();
			
			gsList = gsList.parallelStream()
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
					.collect(Collectors.toList());
			
			List<IChannel> channels = new ArrayList<>();
					
			for (GuildSettings gs : gsList) {
				IChannel c = broadcastChannel.apply(gs);
				if (c != null) {
					channelToGS.put(c.getLongID(), gs);
					channels.add(c);
				}
			}
			
			long prepTime = System.currentTimeMillis() - beginMillis;
			
			this.onDone = results -> {
				long successful = results.stream().filter(x -> x != null).count();
				long failed = results.stream().filter(x -> x == null).count();
				
				UltimateGDBot.logSuccess("Received broadcast results for GD event **" + eventName + "** for " 
					+ componentToHumanReadableString(component) + ".\n"
					+ "Total guilds that were subscribed to **" + eventName + "** event: **" + channels.size() + "**\n"
					+ "Successfully notified: " + successful + " guilds\n"
					+ "Failed to notify: " + failed + " guilds\n"
					+ "**Total execution time: " + BotUtils.formatTimeMillis(System.currentTimeMillis() - beginMillis) + "**");
			};
			
			this.onBroadcastDone = () -> {
				long broadcastTime = System.currentTimeMillis() - beginMillis - prepTime;
				
				UltimateGDBot.logSuccess("Successfully processed GD event **" + eventName + "** for " 
						+ componentToHumanReadableString(component) + ".\n"
						+ "Gathered info on guilds subscribed to this event in: " + BotUtils.formatTimeMillis(prepTime) + "\n"
						+ "Sent message requests in: " + BotUtils.formatTimeMillis(broadcastTime) + "\n"
						+ "Now waiting for broadcast results...");
			};
			
			this.broadcastComponent(component, channels, channelToGS);
			
			this.executeAfter(component);
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
