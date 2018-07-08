package com.github.alex1304.ultimategdbot.modules.gdevents.broadcast;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IRole;

/**
 * Builds a message with an optional role to tag at the beginning of the message
 *
 * @author Alex1304
 */
public class OptionalRoleTagMessage implements BroadcastableMessage {

	private String baseContent;
	private EmbedObject baseEmbed;
	private IRole roleToPing;
	
	public OptionalRoleTagMessage(String baseContent, EmbedObject baseEmbed, IRole roleToPing) {
		this.baseContent = baseContent;
		this.baseEmbed = baseEmbed;
		this.roleToPing = roleToPing;
	}

	@Override
	public String buildContent() {
		StringBuffer sb = new StringBuffer();
		
		if (roleToPing != null)
			sb.append(roleToPing.mention() + " ");
		
		return sb.append(baseContent).toString();
	}

	@Override
	public EmbedObject buildEmbed() {
		return baseEmbed;
	}

	/**
	 * Gets the baseContent
	 *
	 * @return String
	 */
	public String getBaseContent() {
		return baseContent;
	}

	/**
	 * Gets the baseEmbed
	 *
	 * @return EmbedObject
	 */
	public EmbedObject getBaseEmbed() {
		return baseEmbed;
	}

	/**
	 * Gets the roleToPing
	 *
	 * @return IRole
	 */
	public IRole getRoleToPing() {
		return roleToPing;
	}

}
