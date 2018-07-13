package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import com.github.alex1304.jdash.component.property.GDUserRole;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;

/**
 * Message when user is promoted Elder moderator
 *
 * @author Alex1304
 */
public class UserModStatusMessage extends OptionalRoleTagMessage {

	public UserModStatusMessage(boolean promoted, GDUserRole role) {
		this(null, null, promoted, role);
	}

	public UserModStatusMessage(EmbedObject baseEmbed, IRole roleToPing, boolean promoted, GDUserRole role) {
		super("A user has been " + (promoted ? "promoted" : "demoted") +
				(role == GDUserRole.USER ? "" : " to Geometry Dash " + (role == GDUserRole.ELDER_MODERATOR ?
						"Elder " : "") + "Moderator") + (promoted ? "!" : "..."), baseEmbed, roleToPing);
	}
}
