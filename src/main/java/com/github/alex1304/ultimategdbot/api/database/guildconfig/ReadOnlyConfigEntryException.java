package com.github.alex1304.ultimategdbot.api.database.guildconfig;

public class ReadOnlyConfigEntryException extends RuntimeException {
	private static final long serialVersionUID = 5446399551473841343L;
	
	private final String key;

	public ReadOnlyConfigEntryException(String key) {
		super(key);
		this.key = key;
	}

	/**
	 * Gets the configuration entry key that caused this exception.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
}
