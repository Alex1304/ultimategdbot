package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.RequestBuffer;

/**
 * Entry point of the program
 *
 * @author Alex1304
 */
public class Main {
	
	public static void main(String[] args) throws Exception {
		Database.init();
		UltimateGDBot.init();
		UltimateGDBot.client().getDispatcher().registerListener(ON_READY);
		
		UltimateGDBot.addModule("commands", new CommandsModule());
		UltimateGDBot.addModule("reply", new ReplyModule());
		
		UltimateGDBot.client().login();
	}
	
	private static final IListener<ReadyEvent> ON_READY = event -> {
		try {
			UltimateGDBot.startModules();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		UltimateGDBot.logInfo("Bot started!");
		RequestBuffer.request(() -> {
			UltimateGDBot.client().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "Try out my new command "
					+ UltimateGDBot.property("ultimategdbot.prefix.canonical") + "help !");
		});
	};
}
