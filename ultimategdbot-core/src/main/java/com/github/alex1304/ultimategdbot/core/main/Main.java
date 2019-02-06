package com.github.alex1304.ultimategdbot.core.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.core.impl.BotImpl;

public class Main {

	private static final Path PROPS_FILE = Paths.get(".", "config", "bot.properties");
	private static final Path HIB_PROPS_FILE = Paths.get(".", "config", "hibernate.properties");

	public static void main(String[] args) throws Exception {
		var props = new Properties();
		var hibProps = new Properties();
		try (var input = Files.newInputStream(PROPS_FILE)) {
			props.load(input);
		}
		try (var input = Files.newInputStream(HIB_PROPS_FILE)) {
			hibProps.load(input);
		}
		Bot bot = null;
		try {
			bot = BotImpl.buildFromProperties(props, hibProps);
		} catch (IllegalArgumentException e) {
			System.err.println("Error when parsing " + PROPS_FILE + " file: " + e.getMessage());
		}
		bot.start();	
	}

}
