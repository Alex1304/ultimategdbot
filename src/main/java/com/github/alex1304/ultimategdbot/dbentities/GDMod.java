package com.github.alex1304.ultimategdbot.dbentities;

/**
 * Database entity for GD mod
 *
 * @author Alex1304
 */
public class GDMod {
	
	private long accountID;
	private boolean elder;

	public GDMod() {
	}

	public GDMod(long accountID, boolean elder) {
		this.accountID = accountID;
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
}
