package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.ultimategdbot.dbentities.AwardedLevel;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.MessageBroadcaster;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.OptionalRoleTagMessage;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.EmbedObject.AuthorObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

/**
 * Builds consumer for awarded level events
 *
 * @author Alex1304
 */
public class GDAwardedConsumerBuilder extends GDEventConsumerBuilder<GDComponentList<GDLevelPreview>> {
	
	private AuthorObject embedAuthor;
	private boolean saveResults;
	private Map<Long, List<IMessage>> broadcastResults;
	private Map<Long, Procedure> awardedPendingEdit;

	public GDAwardedConsumerBuilder(String eventName, AuthorObject embedAuthor, Supplier<BroadcastableMessage> messageToBroadcast, boolean saveResults) {
		super(eventName, "channelAwardedLevels", messageToBroadcast);
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

			mb.broadcast();
			
			if (saveResults)
				broadcastResults.put(lp.getId(), mb.getResults());
		}
	}

	@Override
	protected void executeBefore(GDComponentList<GDLevelPreview> component) {
		if (saveResults)
			component.forEach(l -> broadcastResults.put(l.getId(), new ArrayList<>()));
	}

	@Override
	protected void executeAfter(GDComponentList<GDLevelPreview> component) {
		if (saveResults) {
			component.forEach(l -> {
				Procedure doEdit = awardedPendingEdit.get(l.getId());
				if (doEdit != null)
					doEdit.run();
				awardedPendingEdit.remove(l.getId());
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
	 * @return Map<Long,List<IMessage>>
	 */
	public Map<Long, List<IMessage>> getBroadcastResults() {
		return broadcastResults;
	}

	/**
	 * Gets the awardedPendingEdit
	 *
	 * @return Map<Long,Procedure>
	 */
	public Map<Long, Procedure> getAwardedPendingEdit() {
		return awardedPendingEdit;
	}
	
}
