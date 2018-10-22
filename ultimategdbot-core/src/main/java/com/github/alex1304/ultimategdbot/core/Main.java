package com.github.alex1304.ultimategdbot.core;

import java.util.Properties;

import org.xeustechnologies.jcl.JarClassLoader;

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
		var classloader = new JarClassLoader();
		classloader.add("./plugins/");
		classloader.add("./dependency/");
		
		var handler = new DiscordCommandHandler(bot, classloader);
		handler.bind();
		
		bot.getDiscordClient().login().block();
	}
}
