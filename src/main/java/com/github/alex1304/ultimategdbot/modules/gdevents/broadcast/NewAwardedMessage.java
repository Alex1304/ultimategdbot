package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;

/**
 * Broadcastable message for awarded levels
 *
 * @author Alex1304
 */
public class NewAwardedMessage extends OptionalRoleTagMessage {
	
	private static final List<String> RANDOM_TEXT = new ArrayList<>(Arrays.asList(
			"A new level has just been rated on Geometry Dash!!!",
			"RobTop just assigned a star value to this level!",
			"This level can now give you star and orb rewards. Go beat it now!",
			"I've been told that another generic and bad level got rated... Oh well, I might be wrong, go see by yourself!",
			"I challenge you to beat this level. RobTop just rated it!",
			"This level is 2hard5me. But RobTop's rate button has spoken and it can now give you some cool rewards!",
			"RobTop accidentally hit the wrong key and rated this level instead of turning on his coffee machine. But it's actually a good level. Go check it out!",
			"Hey look, a new level got rated OwO Do you think you can beat it?",
			"Roses are red. Violets are blue. This newly awarded level is waiting for you."
	));

	public NewAwardedMessage(EmbedObject baseEmbed, IRole roleToPing) {
		super(RANDOM_TEXT.get(new Random().nextInt(RANDOM_TEXT.size())), "Congratulations for getting your level rated!", baseEmbed, roleToPing);
	}
	
	public NewAwardedMessage() {
		this(null, null);
	}
}
