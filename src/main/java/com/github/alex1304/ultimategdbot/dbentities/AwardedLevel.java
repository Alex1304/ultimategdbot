package com.github.alex1304.ultimategdbot.dbentities;

import java.sql.Timestamp;

/**
 * Database entity for awarded level
 *
 * @author Alex1304
 */
public class AwardedLevel {

	private long levelID;
	private Timestamp insertDate;
	private int downloads;
	private int likes;
	
	public AwardedLevel() {
	}
	
	public AwardedLevel(long levelID, Timestamp insertDate, int downloads, int likes) {
		this.levelID = levelID;
		this.insertDate = insertDate;
		this.downloads = downloads;
		this.likes = likes;
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
	 * Gets the downloads
	 *
	 * @return int
	 */
	public int getDownloads() {
		return downloads;
	}

	/**
	 * Gets the likes
	 *
	 * @return int
	 */
	public int getLikes() {
		return likes;
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
	 * Sets the downloads
	 *
	 * @param downloads - int
	 */
	public void setDownloads(int downloads) {
		this.downloads = downloads;
	}

	/**
	 * Sets the likes
	 *
	 * @param likes - int
	 */
	public void setLikes(int likes) {
		this.likes = likes;
	}
}
