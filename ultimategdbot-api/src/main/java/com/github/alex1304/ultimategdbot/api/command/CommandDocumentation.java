package com.github.alex1304.ultimategdbot.api.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class CommandDocumentation {

	private final String shortDescription;
	private final Map<String, CommandDocumentationEntry> docEntries;

	public CommandDocumentation(String shortDescription, Map<String, CommandDocumentationEntry> docEntries) {
		this.shortDescription = Objects.requireNonNull(shortDescription);
		this.docEntries = Collections.unmodifiableMap(new TreeMap<>(docEntries));
	}
	
	public String getShortDescription() {
		return shortDescription;
	}

	public Map<String, CommandDocumentationEntry> getEntries() {
		return docEntries;
	}
}
