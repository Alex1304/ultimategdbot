package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;

/**
 * Broadcastable message for Daily level changed
 *
 * @author Alex1304
 */
public class TimelyChangedMessage extends OptionalRoleTagMessage {
	
	private static final String TEXT = "There is a new %s on Geometry Dash!!!";
	
	private boolean weekly;
	
	public TimelyChangedMessage(boolean weekly) {
		this(null, null, weekly);
	}

	public TimelyChangedMessage(EmbedObject baseEmbed, IRole roleToPing, boolean weekly) {
		super(String.format(TEXT, weekly ? "Weekly demon" : "Daily level"), baseEmbed, roleToPing);
		this.weekly = weekly;
	}

	/**
	 * Gets the weekly
	 *
	 * @return boolean
	 */
	public boolean isWeekly() {
		return weekly;
	}
}
