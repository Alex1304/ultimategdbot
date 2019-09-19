package com.github.alex1304.ultimategdbot.api.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CommandDocumentationEntry {

	private final String syntax;
	private final String description;
	private final Map<String, FlagInformation> flagInfo;
	
	public CommandDocumentationEntry(String syntax, String description, Map<String, FlagInformation> flagInfo) {
		this.syntax = Objects.requireNonNull(syntax);
		this.description = Objects.requireNonNull(description);
		this.flagInfo = Collections.unmodifiableMap(flagInfo);
	}

	public String getSyntax() {
		return syntax;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, FlagInformation> getFlagInfo() {
		return flagInfo;
	}
}
