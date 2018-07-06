package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
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
			UltimateGDBot.loadEmojiGuilds();
			UltimateGDBot.startModules();
			
			// Subscribe commands and reply modules to message received events
			UltimateGDBot.client().getDispatcher().registerListener((IListener<MessageReceivedEvent>) event0 -> {
				try {
					CommandsModule cm = (CommandsModule) UltimateGDBot.getModule("commands");
					cm.onMessageReceived(event0);
				} catch (ModuleUnavailableException e) {}
				try {
					ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
					rm.onMessageReceived(event0);
				} catch (ModuleUnavailableException e) {}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		UltimateGDBot.logInfo("Bot started!");
		RequestBuffer.request(() -> {
			UltimateGDBot.client().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "Geometry Dash | "
					+ UltimateGDBot.property("ultimategdbot.prefix.canonical") + "help");
		});
	};
}
