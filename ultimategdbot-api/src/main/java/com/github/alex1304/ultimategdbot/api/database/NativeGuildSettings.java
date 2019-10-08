package com.github.alex1304.ultimategdbot.api.database;

import static java.util.Objects.requireNonNullElse;

public class NativeGuildSettings implements GuildSettings {
	
	private long guildId;
	private String prefix;
	private long serverModRoleId;
	
	@Override
	public long getGuildId() {
		return guildId;
	}

	@Override
	public void setGuildId(Long guildId) {
		this.guildId = requireNonNullElse(guildId, 0L);
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public long getServerModRoleId() {
		return serverModRoleId;
	}

	public void setServerModRoleId(Long serverModRoleId) {
		this.serverModRoleId = requireNonNullElse(serverModRoleId, 0L);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof NativeGuildSettings && ((NativeGuildSettings) obj).guildId == guildId;
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(guildId);
	}
}
