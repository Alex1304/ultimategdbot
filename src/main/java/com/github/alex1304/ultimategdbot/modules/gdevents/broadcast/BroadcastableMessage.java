package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import sx.blah.discord.api.internal.json.objects.EmbedObject;

/**
 * Contains info on a message that can be broadcasted (aka sent to multiple servers).
 *
 * @author Alex1304
 */
public interface BroadcastableMessage {
	
	/**
	 * Builds and returns the content of the message
	 * 
	 * @return String
	 */
	String buildContent();
	
	/**
	 * Builds and returns the embed of the message
	 * 
	 * @return EmbedObject
	 */
	EmbedObject buildEmbed();
}
