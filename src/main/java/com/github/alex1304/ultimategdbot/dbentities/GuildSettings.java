package com.github.alex1304.ultimategdbot.dbentities;

/**
 * Database entity for guild settings
 *
 * @author Alex1304
 */
public class GuildSettings {
	
	private long guildID;
	private long roleAwardedLevels;
	private long channelAwardedLevels;
	private long roleGDModerators;
	private long channelGDModerators;
	private long channelTimelyLevels;
	private long roleTimelyLevels;
	private long channelBotAnnouncements;
	private boolean tagEveryoneOnBotAnnouncement;

	public GuildSettings() {
	}

	/**
	 * @param guildID
	 * @param roleAwardedLevels
	 * @param channelAwardedLevels
	 * @param roleGDModerators
	 * @param channelGDModerators
	 * @param channelTimelyLevels
	 * @param roleTimelyLevels
	 * @param channelBotAnnouncements
	 * @param tagEveryoneOnBotAnnouncement
	 */
	public GuildSettings(int guildID, long roleAwardedLevels, long channelAwardedLevels, long roleGDModerators,
			long channelGDModerators, long channelTimelyLevels, long roleTimelyLevels, long channelBotAnnouncements,
			boolean tagEveryoneOnBotAnnouncement) {
		this.guildID = guildID;
		this.roleAwardedLevels = roleAwardedLevels;
		this.channelAwardedLevels = channelAwardedLevels;
		this.roleGDModerators = roleGDModerators;
		this.channelGDModerators = channelGDModerators;
		this.channelTimelyLevels = channelTimelyLevels;
		this.roleTimelyLevels = roleTimelyLevels;
		this.channelBotAnnouncements = channelBotAnnouncements;
		this.tagEveryoneOnBotAnnouncement = tagEveryoneOnBotAnnouncement;
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

	/**
	 * Gets the channelBotAnnouncements
	 *
	 * @return long
	 */
	public long getChannelBotAnnouncements() {
		return channelBotAnnouncements;
	}

	/**
	 * Sets the channelBotAnnouncements
	 *
	 * @param channelBotAnnouncements - long
	 */
	public void setChannelBotAnnouncements(long channelBotAnnouncements) {
		this.channelBotAnnouncements = channelBotAnnouncements;
	}

	/**
	 * Gets the tagEveryoneOnBotAnnouncement
	 *
	 * @return boolean
	 */
	public boolean isTagEveryoneOnBotAnnouncement() {
		return tagEveryoneOnBotAnnouncement;
	}

	/**
	 * Sets the tagEveryoneOnBotAnnouncement
	 *
	 * @param tagEveryoneOnBotAnnouncement - boolean
	 */
	public void setTagEveryoneOnBotAnnouncement(boolean tagEveryoneOnBotAnnouncement) {
		this.tagEveryoneOnBotAnnouncement = tagEveryoneOnBotAnnouncement;
	}

	@Override
	public String toString() {
		return "GuildSettings [guildID=" + guildID + ", roleAwardedLevels=" + roleAwardedLevels
				+ ", channelAwardedLevels=" + channelAwardedLevels + ", roleGDModerators=" + roleGDModerators
				+ ", channelGDModerators=" + channelGDModerators + ", channelTimelyLevels=" + channelTimelyLevels
				+ ", roleTimelyLevels=" + roleTimelyLevels + ", channelBotAnnouncements=" + channelBotAnnouncements
				+ ", tagEveryoneOnBotAnnouncement=" + tagEveryoneOnBotAnnouncement + "]";
	}

}
