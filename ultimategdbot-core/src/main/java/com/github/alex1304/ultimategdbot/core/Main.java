package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;

import com.github.alex1304.ultimategdbot.database.Database;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;

/**
 * Entry point of the program. Loads properties and starts services.
 * 
 * @author Alex1304
 */
public class Main {
	
	static final String PROPS_FILE = "/ultimategdbot.properties";
	
	public static void main(String[] args) throws Exception {
		final var props = new Properties();
		props.load(Main.class.getResourceAsStream(PROPS_FILE));
		Database.init();
		
		final var bot = UltimateGDBot.buildFromProperties(props);
		final var cmdLoader = new CommandPluginLoader();
		final var srvLoader = new ServicePluginLoader();
		final var nativeLoader = new NativeCommandLoader(cmdLoader, srvLoader);
		
		cmdLoader.bind(bot);
		srvLoader.bind(bot);
		nativeLoader.bind(bot);
		
		bot.getDiscordClient().login().block();
	}
}
