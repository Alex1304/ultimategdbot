package com.github.alex1304.ultimategdbot.api.command;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a documentation entry. An entry is a subsection of the
 * documentation of the command. An entry shows one possible syntax for the
 * command, a description of what this part of the command does, and the
 * different flags that can be used.
 */
public class CommandDocumentationEntry {

	private final String syntax;
	private final String description;
	private final Map<String, FlagInformation> flagInfo;
	
	public CommandDocumentationEntry(String syntax, String description, Map<String, FlagInformation> flagInfo) {
		this.syntax = Objects.requireNonNull(syntax);
		this.description = Objects.requireNonNull(description);
		this.flagInfo = Collections.unmodifiableMap(flagInfo);
	}

	/**
	 * Gets the syntax of the command relevant for this entry.
	 *  
	 * @return the syntax
	 */
	public String getSyntax() {
		return syntax;
	}
	
	/**
	 * Gets the description of what the command does, according to the usage
	 * represented by this entry.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets information on the flags that can be used with this part of the command.
	 * 
	 * @return the information on available flags
	 */
	public Map<String, FlagInformation> getFlagInfo() {
		return flagInfo;
	}
}
