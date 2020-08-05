package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.github.alex1304.ultimategdbot.api.Translator;

/**
 * Holds the documentation for a specific command.
 */
public final class CommandDocumentation {

	private final DocumentationLocaleAdapter docLocaleAdapter;
	private final String shortDescription;
	private final Map<String, CommandDocumentationEntry> docEntries;
	private final boolean isHidden;

	public CommandDocumentation(Translator translator, String shortDescription, Map<String, CommandDocumentationEntry> docEntries, boolean isHidden) {
		this.docLocaleAdapter = new DocumentationLocaleAdapter(requireNonNull(translator));
		this.shortDescription = Objects.requireNonNull(shortDescription);
		this.docEntries = Collections.unmodifiableMap(new TreeMap<>(docEntries));
		this.isHidden = isHidden;
	}
	
	/**
	 * Gets the short description of the command.
	 * 
	 * @return the short description
	 */
	public String getShortDescription() {
		return docLocaleAdapter.adapt(shortDescription);
	}
	
	/**
	 * Gets all entries corresponding to subsections of the command documentation.
	 * 
	 * @return the documentation entries
	 */
	public Map<String, CommandDocumentationEntry> getEntries() {
		return docEntries;
	}

	/**
	 * Gets whether the command should be hidden from the documentation front page.
	 * 
	 * @return true if hidden, false otherwise
	 */
	public boolean isHidden() {
		return isHidden;
	}
}
