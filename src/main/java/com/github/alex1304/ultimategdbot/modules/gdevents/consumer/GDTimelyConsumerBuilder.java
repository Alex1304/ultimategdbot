package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.alex1304.jdash.component.GDTimelyLevel;
import com.github.alex1304.jdashevents.customcomponents.GDUpdatedComponent;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.dbentities.TimelyLevel;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleTimelyLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.MessageBroadcaster;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.OptionalRoleTagMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.EmbedObject.AuthorObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * Builds consumer for timely level events
 *
 * @author Alex1304
 */
public class GDTimelyConsumerBuilder extends GDEventConsumerBuilder<GDUpdatedComponent<GDTimelyLevel>> {

	private Function<Long, AuthorObject> embedAuthor;

	public GDTimelyConsumerBuilder(String eventName, Function<Long, AuthorObject> embedAuthor, Supplier<BroadcastableMessage> messageToBroadcast) {
		super(eventName, "channelTimelyLevels", messageToBroadcast);
		this.embedAuthor = embedAuthor;
	}

	@Override
	protected void broadcastComponent(GDUpdatedComponent<GDTimelyLevel> component, List<IChannel> channels,
			Map<Long, GuildSettings> channelToGS) {
		
		AuthorObject ao = embedAuthor.apply(component.getAfterUpdate().getTimelyNumber());
		EmbedObject embed = GDUtils.buildEmbedForGDLevel(ao, component.getAfterUpdate());

		MessageBroadcaster mb = new MessageBroadcaster(channels, channel -> {
			GuildSettings gs = channelToGS.get(channel.getLongID());
			RoleTimelyLevelsSetting rals = new RoleTimelyLevelsSetting(gs);

			OptionalRoleTagMessage ortm = (OptionalRoleTagMessage) messageToBroadcast.get();
			ortm.setBaseEmbed(embed);
			ortm.setRoleToPing(rals.getValue());
			return ortm;
		});

		mb.broadcast();
		List<IUser> linkedUsers = GDUtils.getDiscordUsersLinkedToGDAccount(GDUtils.guessGDUserIDFromString(component.getAfterUpdate().getCreatorName()));
		linkedUsers.forEach(u -> BotUtils.sendMessage(u.getOrCreatePMChannel(), ((OptionalRoleTagMessage) messageToBroadcast.get()).getPrivateContent(), embed));
	}
	
	@Override
	protected void executeAfter(GDUpdatedComponent<GDTimelyLevel> component) {
		DatabaseUtils.save(new TimelyLevel(component.getAfterUpdate().getId(), new Timestamp(System.currentTimeMillis()),
						component.getAfterUpdate().isWeekly()));
	}
	
	@Override
	protected String componentToHumanReadableString(GDUpdatedComponent<GDTimelyLevel> component) {
		return "level __" + component.getAfterUpdate().getName() + "__ by " + component.getAfterUpdate().getCreatorName()
				+ " (" + component.getAfterUpdate().getId() + ") ";
	}
}
