package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.dbentities.AwardedLevel;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.MessageBroadcaster;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.OptionalRoleTagMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.EmbedObject.AuthorObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

/**
 * Builds consumer for awarded level events
 *
 * @author Alex1304
 */
public class GDAwardedConsumerBuilder extends GDEventConsumerBuilder<GDComponentList<GDLevelPreview>> {
	
	private AuthorObject embedAuthor;
	private boolean saveResults;
	private Map<Long, ObservableList<? extends IMessage>> broadcastResults;
	private Map<Long, Procedure> awardedPendingEdit;

	public GDAwardedConsumerBuilder(String eventName, AuthorObject embedAuthor, Supplier<BroadcastableMessage> messageToBroadcast, boolean saveResults) {
		super(eventName, "channelAwardedLevels", messageToBroadcast, gs -> new ChannelAwardedLevelsSetting(gs).getValue());
		this.embedAuthor = embedAuthor;
		this.saveResults = saveResults;
		this.broadcastResults = new ConcurrentHashMap<>();
		this.awardedPendingEdit = new ConcurrentHashMap<>();
	}

	@Override
	protected void broadcastComponent(GDComponentList<GDLevelPreview> component, List<IChannel> channels,
			Map<Long, GuildSettings> channelToGS) {

		for (GDLevelPreview lp : component) {
			EmbedObject embed = GDUtils.buildEmbedForGDLevel(embedAuthor, lp);

			MessageBroadcaster mb = new MessageBroadcaster(channels, channel -> {
				GuildSettings gs = channelToGS.get(channel.getLongID());
				RoleAwardedLevelsSetting rals = new RoleAwardedLevelsSetting(gs);

				OptionalRoleTagMessage ortm = (OptionalRoleTagMessage) messageToBroadcast.get();
				ortm.setBaseEmbed(embed);
				ortm.setRoleToPing(rals.getValue());
				return ortm;
			});
			
			mb.setOnDone(results -> {
				// Do nothing if the component isn't the last one
				if (component.get(component.size() - 1).equals(lp)) {
					this.onDone.accept(results);
					if (saveResults) {
						broadcastResults.put(lp.getId(), results);
						component.forEach(l -> {
							Procedure doEdit = awardedPendingEdit.get(l.getId());
							if (doEdit != null)
								doEdit.run();
							awardedPendingEdit.remove(l.getId());
						});
					}
				}
			});
			
			GDUser creator = GDUtils.guessGDUserFromString(lp.getCreatorName());
			List<IUser> linkedUsers = GDUtils.getDiscordUsersLinkedToGDAccount(creator == null ? -1 : creator.getAccountID());
			linkedUsers.forEach(u -> BotUtils.sendMessage(u.getOrCreatePMChannel(), ((OptionalRoleTagMessage) messageToBroadcast.get()).getPrivateContent(), embed));

			mb.broadcast();
		}
		
		onBroadcastDone.run();
	}

	@Override
	protected void executeBefore(GDComponentList<GDLevelPreview> component) {
		if (saveResults)
			component.forEach(l -> broadcastResults.put(l.getId(), FXCollections.observableArrayList()));
	}

	@Override
	protected void executeAfter(GDComponentList<GDLevelPreview> component) {
		if (saveResults) {
			component.forEach(l -> {
				DatabaseUtils.save(new AwardedLevel(l.getId(), new Timestamp(System.currentTimeMillis()),
						l.getDownloads(), l.getLikes()));
			});
		}
	}

	@Override
	protected String componentToHumanReadableString(GDComponentList<GDLevelPreview> component) {
		StringBuffer sb = new StringBuffer("levels\n");
		
		component.forEach(l -> sb.append("- __" + l.getName() + "__ by " + l.getCreatorName()
				+ " (" + l.getId() + ")\n"));
		
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
	
	/**
	 * Gets the broadcastResults
	 *
	 * @return Map&lt;Long, ObservableList&lt;? extends IMessage&gt;&gt;
	 */
	public Map<Long, ObservableList<? extends IMessage>> getBroadcastResults() {
		return broadcastResults;
	}

	/**
	 * Gets the awardedPendingEdit
	 *
	 * @return Map&lt;Long, Procedure&gt;
	 */
	public Map<Long, Procedure> getAwardedPendingEdit() {
		return awardedPendingEdit;
	}
	
}
