package com.github.alex1304.ultimategdbot.api.command;

/**
 * Holds information about a command flag.
 */
public class FlagInformation {

	private final String valueFormat;
	private final String description;
	
	public FlagInformation(String valueFormat, String description) {
		this.valueFormat = valueFormat;
		this.description = description;
	}

	/**
	 * Gets a description of the value format.
	 * 
	 * @return the value format
	 */
	public String getValueFormat() {
		return valueFormat;
	}

	/**
	 * Gets a description of what this flag does for the command.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
}
