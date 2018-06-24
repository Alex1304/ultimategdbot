package com.github.alex1304.ultimategdbot.cache;

/**
 * Represents a cache entry
 *
 * @author Alex1304
 */
public class CacheEntry {

	private Object item;
	private long expiryTimestamp;
	
	/**
	 * @param item
	 * @param expiryTimestamp
	 */
	public CacheEntry(Object item, long expiryTimestamp) {
		this.item = item;
		this.expiryTimestamp = expiryTimestamp;
	}
	
	/**
	 * Gets the item
	 *
	 * @return Object
	 */
	public Object getItem() {
		return item;
	}
	/**
	 * Gets the expiryTimestamp
	 *
	 * @return long
	 */
	public long getExpiryTimestamp() {
		return expiryTimestamp;
	}

}
