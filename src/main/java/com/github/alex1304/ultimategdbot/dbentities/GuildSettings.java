package com.github.alex1304.ultimategdbot.dbentities;

import sx.blah.discord.handle.obj.IGuild;

/**
 * Database entity for guild settings
 *
 * @author Alex1304
 */
public class GuildSettings {
	
	private IGuild guildInstance;
	private long guildID;
	private long roleAwardedLevels;
	private long channelAwardedLevels;
	private long roleGDModerators;
	private long channelGDModerators;
	private long channelTimelyLevels;
	private long roleTimelyLevels;
	private long channelChangelog;

	public GuildSettings() {
	}

	public GuildSettings(long guildID, long roleAwardedLevels, long channelAwardedLevels, long roleGDModerators,
			long channelGDModerators, long channelTimelyLevels, long roleTimelyLevels, long channelChangelog) {
		this.guildID = guildID;
		this.roleAwardedLevels = roleAwardedLevels;
		this.channelAwardedLevels = channelAwardedLevels;
		this.roleGDModerators = roleGDModerators;
		this.channelGDModerators = channelGDModerators;
		this.channelTimelyLevels = channelTimelyLevels;
		this.roleTimelyLevels = roleTimelyLevels;
		this.channelChangelog = channelChangelog;
	}

	/**
	 * Gets the guildID
	 *
	 * @return long
	 */
	public long getGuildID() {
		return guildID;
	}

	/**
	 * Sets the guildID
	 *
	 * @param guildID - long
	 */
	public void setGuildID(long guildID) {
		this.guildID = guildID;
	}

	/**
	 * Gets the roleAwardedLevels
	 *
	 * @return long
	 */
	public long getRoleAwardedLevels() {
		return roleAwardedLevels;
	}

	/**
	 * Sets the roleAwardedLevels
	 *
	 * @param roleAwardedLevels - long
	 */
	public void setRoleAwardedLevels(long roleAwardedLevels) {
		this.roleAwardedLevels = roleAwardedLevels;
	}

	/**
	 * Gets the channelAwardedLevels
	 *
	 * @return long
	 */
	public long getChannelAwardedLevels() {
		return channelAwardedLevels;
	}

	/**
	 * Sets the channelAwardedLevels
	 *
	 * @param channelAwardedLevels - long
	 */
	public void setChannelAwardedLevels(long channelAwardedLevels) {
		this.channelAwardedLevels = channelAwardedLevels;
	}

	/**
	 * Gets the roleGDModerators
	 *
	 * @return long
	 */
	public long getRoleGDModerators() {
		return roleGDModerators;
	}

	/**
	 * Sets the roleGDModerators
	 *
	 * @param roleGDModerators - long
	 */
	public void setRoleGDModerators(long roleGDModerators) {
		this.roleGDModerators = roleGDModerators;
	}

	/**
	 * Gets the channelGDModerators
	 *
	 * @return long
	 */
	public long getChannelGDModerators() {
		return channelGDModerators;
	}

	/**
	 * Sets the channelGDModerators
	 *
	 * @param channelGDModerators - long
	 */
	public void setChannelGDModerators(long channelGDModerators) {
		this.channelGDModerators = channelGDModerators;
	}

	/**
	 * Gets the channelTimelyLevels
	 *
	 * @return long
	 */
	public long getChannelTimelyLevels() {
		return channelTimelyLevels;
	}

	/**
	 * Sets the channelTimelyLevels
	 *
	 * @param channelTimelyLevels - long
	 */
	public void setChannelTimelyLevels(long channelTimelyLevels) {
		this.channelTimelyLevels = channelTimelyLevels;
	}

	/**
	 * Gets the roleTimelyLevels
	 *
	 * @return long
	 */
	public long getRoleTimelyLevels() {
		return roleTimelyLevels;
	}

	/**
	 * Sets the roleTimelyLevels
	 *
	 * @param roleTimelyLevels - long
	 */
	public void setRoleTimelyLevels(long roleTimelyLevels) {
		this.roleTimelyLevels = roleTimelyLevels;
	}

	@Override
	public String toString() {
		return "GuildSettings [guildID=" + guildID + ", roleAwardedLevels=" + roleAwardedLevels
				+ ", channelAwardedLevels=" + channelAwardedLevels + ", roleGDModerators=" + roleGDModerators
				+ ", channelGDModerators=" + channelGDModerators + ", channelTimelyLevels=" + channelTimelyLevels
				+ ", roleTimelyLevels=" + roleTimelyLevels + "]";
	}
	
	/**
	 * Gets the guildInstance
	 *
	 * @return IGuild
	 */
	public IGuild getGuildInstance() {
		return guildInstance;
	}

	/**
	 * Sets the guildInstance
	 *
	 * @param guildInstance - IGuild
	 */
	public void setGuildInstance(IGuild guildInstance) {
		this.guildInstance = guildInstance;
		this.guildID = guildInstance.getLongID();
	}

	/**
	 * Gets the channelChangelog
	 *
	 * @return long
	 */
	public long getChannelChangelog() {
		return channelChangelog;
	}

	/**
	 * Sets the channelChangelog
	 *
	 * @param channelChangelog - long
	 */
	public void setChannelChangelog(long channelChangelog) {
		this.channelChangelog = channelChangelog;
	}
}
