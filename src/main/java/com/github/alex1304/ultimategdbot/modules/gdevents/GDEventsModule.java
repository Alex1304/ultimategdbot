package com.github.alex1304.ultimategdbot.modules.gdevents;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.alex1304.jdash.api.request.GDLevelHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevel;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.jdashevents.GDEvent;
import com.github.alex1304.jdashevents.common.CommonEvents;
import com.github.alex1304.jdashevents.manager.GDEventManager;
import com.github.alex1304.jdashevents.scanner.AwardedLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.DailyLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.WeeklyDemonEventScanner;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.GuildSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.MissingPermissionsException;

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
	
	public GDEventsModule() {
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_ADDED, (GDComponentList<GDLevelPreview> newRates) -> {
			UltimateGDBot.logInfo("Event fired!");
			List<EmbedObject> embeds = new ArrayList<>();
			
			for (GDLevelPreview lp : newRates) {
				GDLevel lvl = (GDLevel) UltimateGDBot.cache()
						.readAndWriteIfNotExists("gd.level." + lp.getId(), () -> {
							return UltimateGDBot.gdClient().fetch(new GDLevelHttpRequest(lp.getId()));
						});
				
				if (lvl != null)
					embeds.add(GDUtils.buildEmbedForGDLevel("New rated level!", "https://i.imgur.com/asoMj1W.png", lp, lvl, false));
			}
			
			if (embeds.isEmpty())
				return;
			
			List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g.channelAwardedLevels > 0");
			
			gsList.parallelStream()
				.filter(gs -> UltimateGDBot.client().getGuilds().stream()
									.anyMatch(g -> g.getLongID() == gs.getGuildID()))
				.forEach(gs -> {
					gs.setGuildInstance(UltimateGDBot.client().getGuildByID(gs.getGuildID()));
					ChannelAwardedLevelsSetting cals = GuildSetting.get(ChannelAwardedLevelsSetting.class, "channel_awarded_levels", gs);
					RoleAwardedLevelsSetting rals = GuildSetting.get(RoleAwardedLevelsSetting.class, "role_awarded_levels", gs);
					
					IChannel channel = cals.getValue();
					
					if (channel == null)
						return;
					
					IRole role = rals.getValue();
					String mention = role != null ? role.mention() : "";
					
					for (EmbedObject em : embeds) {
						try {
							BotUtils.sendWebhookMessage(channel, mention + " A new level has just been rated on Geometry Dash!!!", em);
						} catch (MissingPermissionsException e) {
							System.err.println(e.getMessage());
						}
					}
				});
				
		}));
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

}
