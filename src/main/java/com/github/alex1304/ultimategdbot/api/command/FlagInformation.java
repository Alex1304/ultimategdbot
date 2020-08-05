package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Objects.requireNonNull;

import com.github.alex1304.ultimategdbot.api.Translator;

/**
 * Holds information about a command flag.
 */
public final class FlagInformation {

	private final DocumentationLocaleAdapter docLocaleAdapter;
	private final String valueFormat;
	private final String description;
	
	public FlagInformation(Translator translator,String valueFormat, String description) {
		this.docLocaleAdapter = new DocumentationLocaleAdapter(requireNonNull(translator));
		this.valueFormat = requireNonNull(valueFormat);
		this.description = requireNonNull(description);
	}

	/**
	 * Gets a description of the value format.
	 * 
	 * @return the value format
	 */
	public String getValueFormat() {
		return docLocaleAdapter.adapt(valueFormat);
	}

	/**
	 * Gets a description of what this flag does for the command.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return docLocaleAdapter.adapt(description);
	}
}
