package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;

import com.github.alex1304.ultimategdbot.core.pluginloader.CommandPluginLoader;

/**
 * Entry point of the program. Loads properties and starts services.
 * 
 * @author Alex1304
 */
public class Main {
	
	public static final String PROPS_FILE = "/ultimategdbot.properties";
	public static final String PLUGINS_DIR = "./plugins/";
	
	public static void main(String[] args) throws Exception {
		var props = new Properties();
		props.load(Main.class.getResourceAsStream(PROPS_FILE));
		
		var bot = UltimateGDBot.buildFromProperties(props);
		
		var cpl = new CommandPluginLoader();
		cpl.bindPluginsToBot(bot);
		cpl.load();
		
		bot.getDiscordClient().login().block();
	}
}
