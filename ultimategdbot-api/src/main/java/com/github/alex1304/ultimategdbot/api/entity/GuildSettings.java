package com.github.alex1304.ultimategdbot.api.entity;

public class GuildSettings {
	
	private long guildId;
	private String prefix;

	public long getGuildId() {
		return guildId;
	}

	public void setGuildId(long guildId) {
		this.guildId = guildId;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
