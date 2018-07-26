package com.github.alex1304.ultimategdbot.modules.gdevents;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.property.GDUserRole;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.jdashevents.GDEvent;
import com.github.alex1304.jdashevents.common.CommonEvents;
import com.github.alex1304.jdashevents.customcomponents.GDUpdatedComponent;
import com.github.alex1304.jdashevents.manager.GDEventManager;
import com.github.alex1304.jdashevents.scanner.AwardedLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.DailyLevelEventScanner;
import com.github.alex1304.jdashevents.scanner.WeeklyDemonEventScanner;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.AwardedDeletedMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.NewAwardedMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.TimelyChangedMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.UserModStatusMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.consumer.GDAwardedConsumerBuilder;
import com.github.alex1304.ultimategdbot.modules.gdevents.consumer.GDModConsumerBuilder;
import com.github.alex1304.ultimategdbot.modules.gdevents.consumer.GDTimelyConsumerBuilder;
import com.github.alex1304.ultimategdbot.utils.AuthorObjects;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
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
	
	private static final long SCAN_PERIOD = 10000;
	
	private Timer gdEventsScanTimer;
	private AwardedLevelEventScanner als;
	private DailyLevelEventScanner dls;
	private WeeklyDemonEventScanner wds;
	
	private static final GDAwardedConsumerBuilder AWARDED_ADDED_CB = new GDAwardedConsumerBuilder(
			"Awarded Level Added", AuthorObjects.awardedLevelAdded(), () -> new NewAwardedMessage(), true);
	
	private static final GDAwardedConsumerBuilder AWARDED_DELETED_CB = new GDAwardedConsumerBuilder(
			"Awarded Level Deleted", AuthorObjects.awardedLevelDeleted(), () -> new AwardedDeletedMessage(), false);
	
	private static final GDTimelyConsumerBuilder DAILY_CHANGED_CB = new GDTimelyConsumerBuilder(
			"Daily Level Changed", num -> AuthorObjects.dailyLevel(num), () -> new TimelyChangedMessage(false));

	private static final GDTimelyConsumerBuilder WEEKLY_CHANGED_CB = new GDTimelyConsumerBuilder(
			"Weekly Demon Changed", num -> AuthorObjects.weeklyDemon(num), () -> new TimelyChangedMessage(true));

	private static final GDModConsumerBuilder USER_PROMOTED_ELDER_CB = new GDModConsumerBuilder(
			"User Promoted Elder", AuthorObjects.userPromoted(), () -> new UserModStatusMessage(true, GDUserRole.ELDER_MODERATOR));

	private static final GDModConsumerBuilder USER_PROMOTED_MOD_CB = new GDModConsumerBuilder(
			"User Promoted Mod", AuthorObjects.userPromoted(), () -> new UserModStatusMessage(true, GDUserRole.MODERATOR));

	private static final GDModConsumerBuilder USER_DEMOTED_MOD_CB = new GDModConsumerBuilder(
			"User Demoted Mod", AuthorObjects.userDemoted(), () -> new UserModStatusMessage(false, GDUserRole.MODERATOR));

	private static final GDModConsumerBuilder USER_DEMOTED_USER_CB = new GDModConsumerBuilder(
			"User Demoted User", AuthorObjects.userDemoted(), () -> new UserModStatusMessage(false, GDUserRole.USER));
	
	public GDEventsModule() {
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_ADDED, AWARDED_ADDED_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_DELETED, AWARDED_DELETED_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.DAILY_LEVEL_CHANGED, DAILY_CHANGED_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.WEEKLY_DEMON_CHANGED, WEEKLY_CHANGED_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>("USER_PROMOTED_ELDER", USER_PROMOTED_ELDER_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>("USER_PROMOTED_MOD", USER_PROMOTED_MOD_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>("USER_DEMOTED_MOD", USER_DEMOTED_MOD_CB.build()));
		GDEventManager.getInstance().registerEvent(new GDEvent<>("USER_DEMOTED_USER", USER_DEMOTED_USER_CB.build()));
		
		GDEventManager.getInstance().registerEvent(new GDEvent<>(CommonEvents.AWARDED_LEVEL_UPDATED, (GDComponentList<GDUpdatedComponent<GDLevelPreview>> updated) -> {
			for (GDUpdatedComponent<GDLevelPreview> ulp : updated) {
				GDLevelPreview lp1 = ulp.getBeforeUpdate();
				GDLevelPreview lp2 = ulp.getAfterUpdate();
				
				List<? extends IMessage> ml = AWARDED_ADDED_CB.getBroadcastResults().get(lp1 != null ? lp1.getId() : lp2.getId());
				
				if (ml != null) {
					EmbedObject embed = GDUtils.buildEmbedForGDLevel(AuthorObjects.awardedLevelAdded(), lp2);
					
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
						AWARDED_ADDED_CB.getAwardedPendingEdit().put(lp2.getId(), doEdit);
				}
			}
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
