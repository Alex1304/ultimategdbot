package com.github.alex1304.ultimategdbot.command.api;

import discord4j.core.object.Embed;

/**
 * View for Discord bot commands
 *
 * @author Alex1304
 *
 */
public class DiscordView {

	private final String content;
	private final Embed embed;
	
	public DiscordView(String content, Embed embed) {
		this.content = content;
		this.embed = embed;
	}
	
	public DiscordView(String content) {
		this(content, null);
	}

	/**
	 * Gets the content
	 *
	 * @return String
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Gets the embed
	 *
	 * @return Embed
	 */
	public Embed getEmbed() {
		return embed;
	}
}
