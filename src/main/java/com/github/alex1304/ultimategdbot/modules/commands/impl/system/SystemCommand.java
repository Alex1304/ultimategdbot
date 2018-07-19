package com.github.alex1304.ultimategdbot.modules.commands.impl.system;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.List;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.SystemUnit;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows bot moderators to monitor the bot's system resources usage
 *
 * @author Alex1304
 */
public class SystemCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		System.gc();
		
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();
		OperatingSystemMXBean osmx = ManagementFactory.getOperatingSystemMXBean();
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("__**Memory resources:**__\n\n");

		sb.append("**Total system RAM available:** " + SystemUnit.format(max) + "\n");
		sb.append("**Current JVM size:** " + SystemUnit.format(total) + "\n");
		sb.append("**Application memory usage:** " + SystemUnit.format(total - free) + " / " + SystemUnit.format(total)
				+ " (" + String.format("%.2f", (total - free) * 100 / (double) total) + "%)\n");

		sb.append("\n__**CPU resources:**__\n\n");
		
		sb.append("**CPU cores available:** " + Runtime.getRuntime().availableProcessors() + "\n");
		
		for (Method method : osmx.getClass().getDeclaredMethods()) {
			method.setAccessible(true);
			String methodName = method.getName();
			if (methodName.startsWith("get") && methodName.contains("Cpu") && methodName.contains("Load")
					&& Modifier.isPublic(method.getModifiers())) {

				double value;
				try {
					value = Double.parseDouble(String.valueOf(method.invoke(osmx))) * 100;
				} catch (Exception e) {
					value = -1;
					UltimateGDBot.logException(e);
				}
				
				sb.append("**" + (methodName.equals("getSystemCpuLoad") ? "Global system" : "Application")
						+ " CPU load:** " + String.format("%.3f", Math.round(value * 1000) / 1000.0) + "%\n");
			}
		}
		
		BotUtils.sendMessage(event.getChannel(), sb.toString());
	}
	
	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.MODERATOR);
	}
}
