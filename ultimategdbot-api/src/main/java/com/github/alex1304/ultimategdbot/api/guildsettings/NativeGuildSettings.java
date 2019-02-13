package com.github.alex1304.ultimategdbot.api.guildsettings;

public class NativeGuildSettings implements GuildSettings {
	
	private long guildId;
	private String prefix;
	private long serverModRoleId;
	
	@Override
	public long getGuildId() {
		return guildId;
	}

	@Override
	public void setGuildId(long guildId) {
		this.guildId = guildId;
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

	public void setServerModRoleId(long serverModRoleId) {
		this.serverModRoleId = serverModRoleId;
	}
}
