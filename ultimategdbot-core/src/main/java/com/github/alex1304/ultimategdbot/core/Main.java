package com.github.alex1304.ultimategdbot.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import reactor.util.Logger;
import reactor.util.Loggers;

import com.github.alex1304.ultimategdbot.api.Bot;

class Main {
	
	private static final Logger LOGGER = Loggers.getLogger(Main.class);
	public static final Path PROPS_FILE = Paths.get(".", "config", "bot.properties");
	public static final Path PLUGINS_PROPS_FILE = Paths.get(".", "config", "plugins.properties");

	public static void main(String[] args) {
		try {
			var props = new Properties();
			var pluginsProps = new Properties();
			try (var propsInput = Files.newBufferedReader(PROPS_FILE);
					var pluginsPropsInput = Files.newBufferedReader(PLUGINS_PROPS_FILE)) {
				props.load(propsInput);
				pluginsProps.load(pluginsPropsInput);
			}
			var bot = Bot.buildFromProperties(props, pluginsProps);
			bot.start();	
		} catch (Exception e) {
			LOGGER.error("The bot could not be started. Make sure that all configuration files are present and have a valid content", e);
			System.exit(1);
		}
	}
}
