package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.github.alex1304.ultimategdbot.api.Translator;

/**
 * Represents a documentation entry. An entry is a subsection of the
 * documentation of the command. An entry shows one possible syntax for the
 * command, a description of what this part of the command does, and the
 * different flags that can be used.
 */
public final class CommandDocumentationEntry {

	private final DocumentationLocaleAdapter docLocaleAdapter;
	private final String syntax;
	private final String description;
	private final Map<String, FlagInformation> flagInfo;
	
	public CommandDocumentationEntry(Translator translator, String syntax, String description, Map<String, FlagInformation> flagInfo) {
		this.docLocaleAdapter = new DocumentationLocaleAdapter(requireNonNull(translator));
		this.syntax = requireNonNull(syntax);
		this.description = requireNonNull(description);
		this.flagInfo = unmodifiableMap(flagInfo);
	}

	/**
	 * Gets the syntax of the command relevant for this entry.
	 *  
	 * @return the syntax
	 */
	public String getSyntax() {
		return docLocaleAdapter.adapt(syntax);
	}
	
	/**
	 * Gets the description of what the command does, according to the usage
	 * represented by this entry.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return docLocaleAdapter.adapt(description);
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
