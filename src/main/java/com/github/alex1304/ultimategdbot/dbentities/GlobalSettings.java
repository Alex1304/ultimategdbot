package com.github.alex1304.ultimategdbot.dbentities;

/**
 * Database entity for global settings
 *
 * @author Alex1304
 */
public class GlobalSettings {
	
	private int id;
	private long channelDebugLogs;

	public GlobalSettings() {
	}

	public GlobalSettings(int id, long channelDebugLogs) {
		this.id = id;
		this.channelDebugLogs = channelDebugLogs;
	}

	/**
	 * Gets the id
	 *
	 * @return int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the id
	 *
	 * @param id - int
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Gets the channelDebugLogs
	 *
	 * @return long
	 */
	public long getChannelDebugLogs() {
		return channelDebugLogs;
	}

	/**
	 * Sets the channelDebugLogs
	 *
	 * @param channelDebugLogs - long
	 */
	public void setChannelDebugLogs(long channelDebugLogs) {
		this.channelDebugLogs = channelDebugLogs;
	}

	@Override
	public String toString() {
		return "GlobalSettings [id=" + id + ", channelDebugLogs=" + channelDebugLogs + "]";
	}

}
