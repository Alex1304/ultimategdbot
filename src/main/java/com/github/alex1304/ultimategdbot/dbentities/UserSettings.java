package com.github.alex1304.ultimategdbot.dbentities;

/**
 * Database entity for user settings
 *
 * @author Alex1304
 */
public class UserSettings {
	
	private long userID;
	private long gdUserID;
	private boolean linkActivated;
	private String confirmationToken;

	public UserSettings() {
	}

	public UserSettings(long userID, long gdUserID, boolean linkActivated, String confirmationToken) {
		this.userID = userID;
		this.gdUserID = gdUserID;
		this.linkActivated = linkActivated;
		this.confirmationToken = confirmationToken;
	}

	/**
	 * Gets the userID
	 *
	 * @return long
	 */
	public long getUserID() {
		return userID;
	}

	/**
	 * Sets the userID
	 *
	 * @param userID - long
	 */
	public void setUserID(long userID) {
		this.userID = userID;
	}

	/**
	 * Gets the gdUserID
	 *
	 * @return long
	 */
	public long getGdUserID() {
		return gdUserID;
	}

	/**
	 * Sets the gdUserID
	 *
	 * @param gdUserID - long
	 */
	public void setGdUserID(long gdUserID) {
		this.gdUserID = gdUserID;
	}

	/**
	 * Gets the linkActivated
	 *
	 * @return boolean
	 */
	public boolean getLinkActivated() {
		return linkActivated;
	}

	/**
	 * Sets the linkActivated
	 *
	 * @param linkActivated - boolean
	 */
	public void setLinkActivated(boolean isLinkActivated) {
		this.linkActivated = isLinkActivated;
	}

	/**
	 * Gets the confirmationToken
	 *
	 * @return String
	 */
	public String getConfirmationToken() {
		return confirmationToken;
	}

	/**
	 * Sets the confirmationToken
	 *
	 * @param confirmationToken - String
	 */
	public void setConfirmationToken(String confirmationToken) {
		this.confirmationToken = confirmationToken;
	}

}
