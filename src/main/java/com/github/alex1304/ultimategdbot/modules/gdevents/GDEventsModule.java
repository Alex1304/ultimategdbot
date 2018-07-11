package com.github.alex1304.ultimategdbot.modules.gdevents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.GDTimelyLevel;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.jdashevents.GDEvent;
import com.github.alex1304.jdashevents.common.CommonEvents;
import com.github.alex1304.jdashevents.customcomponents.GDUpdatedComponent;
import com.github.alex1304.jdashevents.manager.GDEventManager;
import com.github.alex1304.jdashevents.scanner.AwardedLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.DailyLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.WeeklyDemonEventScanner;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelTimelyLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.MessageBroadcaster;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.OptionalRoleTagMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

/**
 * Module that manages GD events
 *
 * @author Alex1304
 */
public class GDEventsModule implements Module {
	
	private static final long SCAN_PERIOD = 3000;
	
	private Timer gdEventsScanTimer;
	private AwardedLevelEventScanner als;
	private DailyLevelEventScanner dls;
	private WeeklyDemonEventScanner wds;
	private Map<Long, List<IMessage>> newAwardedBroadcastResults;
	private Map<Long, Procedure> awardedPendingEdit;
	
	public GDEventsModule() {
		this.newAwardedBroadcastResults = new ConcurrentHashMap<>();
		this.awardedPendingEdit = new ConcurrentHashMap<>();
		
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_ADDED,
				broadcastLevelListConsumer("New rated level!", "https://i.imgur.com/asoMj1W.png", 
						"A new level has just been rated on Geometry Dash!!!", 
						"Awarded Level Added", true)));
		
		GDEventManager.getInstance().registerEvent(new GDEvent<GDComponentList<GDUpdatedComponent<GDLevelPreview>>>(CommonEvents.AWARDED_LEVEL_UPDATED, (GDComponentList<GDUpdatedComponent<GDLevelPreview>> updated) -> {
			for (GDUpdatedComponent<GDLevelPreview> ulp : updated) {
				GDLevelPreview lp1 = ulp.getBeforeUpdate();
				GDLevelPreview lp2 = ulp.getAfterUpdate();
				
				List<IMessage> ml = newAwardedBroadcastResults.get(lp1.getId());
				
				if (ml != null) {
					EmbedObject embed = GDUtils.buildEmbedForGDLevel("New rated level!", "https://i.imgur.com/asoMj1W.png", lp2);
					
					Procedure doEdit = () ->
						ml.parallelStream().forEach(m -> {
							RequestBuffer.request(() -> {
								try {
									m.edit(m.getContent(), embed);
								} catch (MissingPermissionsException | DiscordException e) {
								}
							});
						});
						
					if (!ml.isEmpty())
						doEdit.run();
					else
						awardedPendingEdit.put(lp2.getId(), doEdit);
				}
			}
		}));
		
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_DELETED,
				broadcastLevelListConsumer("Level unrated...", "https://i.imgur.com/fPECXUz.png", 
						"A level just got un-rated from Geometry Dash...", 
						"Awarded Level Deleted", false)));

		
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.DAILY_LEVEL_CHANGED,
				broadcastUpdatedLevelConsumer("New Daily level!", "https://i.imgur.com/enpYuB8.png", 
						"There is a new Daily level on Geometry Dash !!!", 
						"Daily Level Changed")));

		
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.WEEKLY_DEMON_CHANGED,
				broadcastUpdatedLevelConsumer("New Weekly demon!", "https://i.imgur.com/kcsP5SN.png", 
						"There is a new Weekly demon on Geometry Dash !!!", 
						"Weekly Demon Changed")));
	}

	@Override
	public void start() {
		this.gdEventsScanTimer = new Timer();
		this.als = new AwardedLevelEventScanner(UltimateGDBot.gdClient());
		this.dls = new DailyLevelEventScanner(UltimateGDBot.gdClient());
		this.wds = new WeeklyDemonEventScanner(UltimateGDBot.gdClient());
		
		gdEventsScanTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					GDEventsModule.this.als.scan();
				} catch (GDAPIException e) {
					UltimateGDBot.logException(e);
				}
			}
			
		}, 0, SCAN_PERIOD * 3);
		
		gdEventsScanTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					GDEventsModule.this.dls.scan();
				} catch (GDAPIException e) {
					UltimateGDBot.logException(e);
				}
			}
			
		}, SCAN_PERIOD, SCAN_PERIOD * 3);
		
		gdEventsScanTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				try {
					GDEventsModule.this.wds.scan();
				} catch (GDAPIException e) {
					UltimateGDBot.logException(e);
				}
			}
			
		}, SCAN_PERIOD * 2, SCAN_PERIOD * 3);
	}

	@Override
	public void stop() {
		gdEventsScanTimer.cancel();
		this.gdEventsScanTimer = null;
		this.als = null;
		this.dls = null;
		this.wds = null;
	}
	
	public Consumer<GDComponentList<GDLevelPreview>> broadcastLevelListConsumer(String authorName, String authorIcon, String messageContent, String eventName, boolean saveResults) {
		return lvllist -> {
			long beginMillis = System.currentTimeMillis();
			
			if (saveResults)
				lvllist.forEach(l -> newAwardedBroadcastResults.put(l.getId(), new ArrayList<>()));
			
			List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g.channelAwardedLevels > 0");
			
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
			
			for (GDLevelPreview lp : lvllist) {
					EmbedObject embed = GDUtils.buildEmbedForGDLevel(authorName, authorIcon, lp);
					
					MessageBroadcaster mb = new MessageBroadcaster(channels, channel -> {
						GuildSettings gs = channelToGS.get(channel.getLongID());
						RoleAwardedLevelsSetting rals = new RoleAwardedLevelsSetting(gs);
						return new OptionalRoleTagMessage(messageContent, embed, rals.getValue());
					});
					
					mb.setOnDone((prepTime, broadcastTime) -> {
						long realPrepTime = prepTime + (System.currentTimeMillis() - beginMillis);
						UltimateGDBot.logSuccess("Successfully processed **" + eventName + "** event for level " + embed.fields[0].name + "\n"
								+ "Gathered info on subscribed guilds in: " + BotUtils.formatTimeMillis(realPrepTime) + "\n"
								+ "Sent messages in all guilds in: " + BotUtils.formatTimeMillis(broadcastTime) + "\n"
								+ "**Total execution time: " + BotUtils.formatTimeMillis(realPrepTime + broadcastTime) + "**");
						
						if (saveResults) {
							newAwardedBroadcastResults.put(lp.getId(), mb.getResults());
							Procedure doEdit = awardedPendingEdit.get(lp.getId());
							if (doEdit != null)
								doEdit.run();
							awardedPendingEdit.remove(lp.getId());
						}
					});
					
					mb.broadcast();
			}
		};
	}
	

	public Consumer<GDUpdatedComponent<GDTimelyLevel>> broadcastUpdatedLevelConsumer(String authorName, String authorIcon, String messageContent, String eventName) {
		return updated -> {
			long beginMillis = System.currentTimeMillis();
			List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g.channelTimelyLevels > 0");
			
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
						IChannel c = new ChannelTimelyLevelsSetting(gs).getValue();
						
						if (c != null)
							channelToGS.put(c.getLongID(), gs);
						return c;
					})
					.filter(c -> c != null)
					.collect(Collectors.toList());
			
			GDTimelyLevel lvl = updated.getAfterUpdate();
			
			EmbedObject embed = GDUtils.buildEmbedForGDLevel(authorName + " (#" + lvl.getTimelyNumber() + ")", authorIcon, lvl);
			
			MessageBroadcaster mb = new MessageBroadcaster(channels, channel -> {
				GuildSettings gs = channelToGS.get(channel.getLongID());
				RoleAwardedLevelsSetting rals = new RoleAwardedLevelsSetting(gs);
				return new OptionalRoleTagMessage(messageContent, embed, rals.getValue());
			});
			
			mb.setOnDone((prepTime, broadcastTime) -> {
				long realPrepTime = prepTime + (System.currentTimeMillis() - beginMillis);
				UltimateGDBot.logSuccess("Successfully processed **" + eventName + "** event for level " + embed.fields[0].name + "\n"
						+ "Gathered info on subscribed guilds in: " + BotUtils.formatTimeMillis(realPrepTime) + "\n"
						+ "Sent messages in all guilds in: " + BotUtils.formatTimeMillis(broadcastTime) + "\n"
						+ "**Total execution time: " + BotUtils.formatTimeMillis(realPrepTime + broadcastTime) + "**");
			});
			
			mb.broadcast();
		};
	}

}
