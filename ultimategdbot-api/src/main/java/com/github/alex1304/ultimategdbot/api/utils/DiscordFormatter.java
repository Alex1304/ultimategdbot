package com.github.alex1304.ultimategdbot.api.utils;

import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;

/**
 * Contains utility methods to format a Discord entity into a user-friendly String.
 */
public class DiscordFormatter {

	private DiscordFormatter() {
	}
	
	/**
	 * Formats a user to the following format: username#discriminator
	 * 
	 * @param user the user to format
	 * @return the formatted user
	 */
	public static String formatUser(User user) {
		return user.getUsername() + "#" + user.getDiscriminator();
	}

	/**
	 * Formats a role to the following format: {@literal @}name (id)
	 * 
	 * @param user the user to format
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
