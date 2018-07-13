package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;

/**
 * 
 *
 * @author Alex1304
 */
public class AwardedDeletedMessage extends OptionalRoleTagMessage {
	
	private static final List<String> RANDOM_TEXT = new ArrayList<>(Arrays.asList(
			"This level just got un-rated from Geometry Dash...",
			"Oh snap! RobTop decided to un-rate this level!",
			"RobTop took away stars from this level. FeelsBadMan",
			"Sad news. This level is no longer rated...",
			"NOOOOOOO I liked this level... No more stars :'("
	));

	public AwardedDeletedMessage(EmbedObject baseEmbed, IRole roleToPing) {
		super("", baseEmbed, roleToPing);
		this.setBaseContent(RANDOM_TEXT.get(new Random().nextInt(RANDOM_TEXT.size())));
	}

	public AwardedDeletedMessage() {
		this.setBaseContent(RANDOM_TEXT.get(new Random().nextInt(RANDOM_TEXT.size())));
	}
}
