package com.github.alex1304.ultimategdbot.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.ServiceLoader;

import com.github.alex1304.ultimategdbot.database.Database;
import com.github.alex1304.ultimategdbot.plugin.api.CommandContainer;
import com.github.alex1304.ultimategdbot.plugin.api.Bot;
import com.github.alex1304.ultimategdbot.plugin.api.Command;

/**
 * Entry point of the program. Loads properties and starts services.
 * 
 * @author Alex1304
 */
class Main {
	
	static final String PROPS_FILE = "./config/bot.properties";
	static final String HIB_PROPS_FILE = "./config/hibernate.properties";
	
	public static void main(String[] args) throws Exception {
		final var props = new Properties();
		final var hibProps = new Properties();
		props.load(new FileInputStream(new File(PROPS_FILE)));
		hibProps.load(new FileInputStream(new File(HIB_PROPS_FILE)));
		Database.setProperties(hibProps);
		final var bot = Bot.buildFromProperties(props);
		final var cmdHandler = new PluginCommandHandler();
		final var nativeHandler = new NativeCommandHandler(cmdHandler);
		
		cmdHandler.bind(bot);
		nativeHandler.bind(bot);

		CommandContainer.getInstance().syncFromLoader(ServiceLoader.load(Command.class));
		CommandContainer.getInstance().forEach(cmd -> {
			cmd.install();
			System.out.println("Loaded initial command: " + cmd.getName() + " [" + cmd.getClass().getName() + "]");
		});

		Database.configure();
		bot.getDiscordClient().login().block();
	}
}
