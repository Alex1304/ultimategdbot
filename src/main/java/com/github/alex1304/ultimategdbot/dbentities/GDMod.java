package com.github.alex1304.ultimategdbot.dbentities;

/**
 * Database entity for GD mod
 *
 * @author Alex1304
 */
public class GDMod {
	
	private long accountID;
	private String username;
	private boolean elder;

	public GDMod() {
	}

	public GDMod(long accountID, String username, boolean elder) {
		this.accountID = accountID;
		this.username = username;
		this.elder = elder;
	}

	/**
	 * Gets the accountID
	 *
	 * @return long
	 */
	public long getAccountID() {
		return accountID;
	}

	/**
	 * Gets the username
	 *
	 * @return String
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Gets the elder
	 *
	 * @return boolean
	 */
	public boolean getElder() {
		return elder;
	}

	/**
	 * Sets the accountID
	 *
	 * @param accountID - long
	 */
	public void setAccountID(long accountID) {
		this.accountID = accountID;
	}

	/**
	 * Sets the elder
	 *
	 * @param elder - boolean
	 */
	public void setElder(boolean elder) {
		this.elder = elder;
	}
	
	/**
	 * Sets the username
	 *
	 * @param username - String
	 */
	public void setUsername(String username) {
		this.username = username;
	}
}
