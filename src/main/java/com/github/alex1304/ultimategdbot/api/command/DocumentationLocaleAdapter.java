package com.github.alex1304.ultimategdbot.api.command;

import com.github.alex1304.ultimategdbot.api.Translator;

class DocumentationLocaleAdapter {
	
	private final Translator translator;

	DocumentationLocaleAdapter(Translator translator) {
		this.translator = translator;
	}
	
	String adapt(String text) {
		if (!text.startsWith("tr:")) {
			return text;
		}
		var stringIdentifier = text.substring(3);
		var tokens = text.substring(3).split("/", 2);
		if (tokens.length != 2) {
			throw new IllegalArgumentException("Invalid string identifier: " + stringIdentifier);
		}
		return translator.translate(tokens[0], tokens[1]);
	}
}
