package com.github.alex1304.ultimategdbot.api.database;

public interface GuildSettings {
	/**
	 * Gets the guildID
	 * 
	 * @return the guildID
	 */
	long getGuildId();
	
	/**
	 * Sets the guildID
	 * 
	 * @param guildId the guildID
	 */
	void setGuildId(Long guildId);
}
