package com.github.alex1304.ultimategdbot.api.command;

public class FlagInformation {

	private final String valueFormat;
	private final String description;
	
	public FlagInformation(String valueFormat, String description) {
		this.valueFormat = valueFormat;
		this.description = description;
	}

	public String getValueFormat() {
		return valueFormat;
	}

	public String getDescription() {
		return description;
	}
}
