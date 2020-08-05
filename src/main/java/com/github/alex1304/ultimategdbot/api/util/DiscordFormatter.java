package com.github.alex1304.ultimategdbot.api.util;

import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;

/**
 * Contains utility methods to format a Discord entity into a user-friendly String.
 */
public final class DiscordFormatter {

	private DiscordFormatter() {
	}

	/**
	 * Formats a role to the following format: {@literal @}name (id)
	 * 
	 * @param role the role to format
	 * @return the formatted user
	 */
	public static String formatRole(Role role) {
		return "@" + role.getName() + " (" + role.getId().asString() + ")";
	}

	/**
	 * Formats a channel to the channel mention if text channel, or name (id) if
	 * voice channel or category.
	 * 
	 * @param channel the channel to format
	 * @return the formatted channel
	 */
	public static String formatGuildChannel(GuildChannel channel) {
		return channel instanceof TextChannel ? channel.getMention()
				: channel.getName() + " (" + channel.getId().asString() + ")";
	}
}
