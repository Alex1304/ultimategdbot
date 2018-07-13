package com.github.alex1304.ultimategdbot.dbentities;

import java.sql.Timestamp;

/**
 * Database entity for awarded level
 *
 * @author Alex1304
 */
public class TimelyLevel {

	private long levelID;
	private Timestamp insertDate;
	private boolean weekly;
	
	public TimelyLevel() {
	}
	
	public TimelyLevel(long levelID, Timestamp insertDate, boolean weekly) {
		this.levelID = levelID;
		this.insertDate = insertDate;
		this.weekly = weekly;
	}

	/**
	 * Gets the levelID
	 *
	 * @return long
	 */
	public long getLevelID() {
		return levelID;
	}

	/**
	 * Gets the insertDate
	 *
	 * @return Timestamp
	 */
	public Timestamp getInsertDate() {
		return insertDate;
	}

	/**
	 * Gets the weekly
	 *
	 * @return boolean
	 */
	public boolean getWeekly() {
		return weekly;
	}

	/**
	 * Sets the levelID
	 *
	 * @param levelID - long
	 */
	public void setLevelID(long levelID) {
		this.levelID = levelID;
	}

	/**
	 * Sets the insertDate
	 *
	 * @param insertDate - Timestamp
	 */
	public void setInsertDate(Timestamp insertDate) {
		this.insertDate = insertDate;
	}

	/**
	 * Sets the weekly
	 *
	 * @param weekly - boolean
	 */
	public void setWeekly(boolean weekly) {
		this.weekly = weekly;
	}
}
