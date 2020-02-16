package com.github.alex1304.ultimategdbot.core.database;

import com.github.alex1304.ultimategdbot.api.database.GuildSettings;

public class GuildPrefixes implements GuildSettings {
	
	private Long guildId;
	private String prefix;
	
	@Override
	public Long getGuildId() {
		return guildId;
	}

	@Override
	public void setGuildId(Long guildId) {
		this.guildId = guildId;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GuildPrefixes && ((GuildPrefixes) obj).guildId == guildId;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(guildId);
	}
}
