package com.github.alex1304.ultimategdbot.modules.commands.impl.restart;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.github.alex1304.ultimategdbot.core.Main;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows bot moderators to restart the bot
 *
 * @author Alex1304
 */
public class RestartCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		try {
			final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			File currentJar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	
			/* is it a jar file? */
			if(!currentJar.getName().endsWith(".jar"))
				return;
	
			/* Build command: java -jar application.jar */
			final List<String> command = new ArrayList<String>();
			command.add(javaBin);
			command.add("-jar");
			command.add(currentJar.getPath());
			
			BotUtils.sendMessage(event.getChannel(), "Restarting...");
			UltimateGDBot.logInfo("Restarting...");
			
			final ProcessBuilder builder = new ProcessBuilder(command);
			builder.redirectErrorStream(true);
			builder.redirectOutput(new File(System.getProperty("user.dir") + File.separator + "restart_" + System.currentTimeMillis() + ".log"));
			builder.start();
			System.exit(0);
		} catch (Exception e) {
			UltimateGDBot.logException(e);
		}
	}

	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.MODERATOR);
	}
}
