package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;

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
		
		bot.getDiscordClient().login().block();
	}
}
