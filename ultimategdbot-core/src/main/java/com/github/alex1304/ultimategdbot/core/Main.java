package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

class Main {
	
	public static final Path PROPS_FILE = Paths.get(".", "config", "bot.properties");
	public static final Path HIB_PROPS_FILE = Paths.get(".", "config", "hibernate.properties");
	public static final Path PLUGINS_PROPS_FILE = Paths.get(".", "config", "plugins.properties");

	public static void main(String[] args) throws Exception {
		var props = new Properties();
		var hibProps = new Properties();
		var pluginsProps = new Properties();
		try (var input = Files.newInputStream(PROPS_FILE)) {
			props.load(input);
		}
		try (var input = Files.newInputStream(HIB_PROPS_FILE)) {
			hibProps.load(input);
		}
		try (var input = Files.newInputStream(PLUGINS_PROPS_FILE)) {
			pluginsProps.load(input);
		}
		BotImpl bot;
		try {
			bot = BotImpl.buildFromProperties(props, hibProps, pluginsProps);
		} catch (IllegalArgumentException e) {
			System.err.println("Error when parsing " + PROPS_FILE + " file: " + e.getMessage());
			return;
		}
		bot.start();	
	}

}
