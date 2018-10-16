package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;
import java.util.ServiceLoader;

import com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
import com.github.alex1304.ultimategdbot.logic.DiscordCommandHandler;
import com.github.alex1304.ultimategdbot.logic.UltimateGDBot;

/**
 * Entry point of the program. Loads properties and starts services.
 * 
 * @author Alex1304
 */
public class Main {
	
	public static final String PROPS_FILE = "/ultimategdbot.properties";
	
	public static void main(String[] args) throws Exception {
		var props = new Properties();
		props.load(Main.class.getResourceAsStream(PROPS_FILE));
		
		var bot = UltimateGDBot.buildFromProperties(props);
		var handler = new DiscordCommandHandler(bot, ServiceLoader.load(DiscordCommand.class));
		handler.bind();
		
		bot.getDiscordClient().login().block();
	}
}
