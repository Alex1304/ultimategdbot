package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

/**
 * Module that manages and handles bot commands
 *
 * @author Alex1304
 */
public class CommandsModule implements Module {

	public static final Map<String, Command> COMMAND_MAP = new HashMap<>();
	
	private boolean isEnabled;

	public CommandsModule() {
		this.isEnabled = false;
	}

	@Override
	public void start() {
		registerCommands();
		isEnabled = true;
		UltimateGDBot.logSuccess("Commands module started!");
		RequestBuffer.request(() -> {
			UltimateGDBot.client().changePresence(StatusType.ONLINE, ActivityType.PLAYING, "I only have a ping command for now");
		});
	}

	@Override
	public void stop() {
		isEnabled = false;
		unregisterCommands();
		UltimateGDBot.logSuccess("Commands module stopped!");
		RequestBuffer.request(() -> {
			UltimateGDBot.client().changePresence(StatusType.IDLE, ActivityType.PLAYING, "Commands unavailable");
		});
	}

	/**
	 * Puts a command into the map, associated by name
	 * @param cmd
	 */
	public void registerCommand(CoreCommand cmd) {
		COMMAND_MAP.put(cmd.getName(), cmd);
	}

	/**
	 * Puts a command into the map, associated by name
	 * @param cmd
	 */
	public void registerCommand(String name, Command cmd) {
		COMMAND_MAP.put(name, cmd);
	}
	
	private void registerCommands() {
		registerCommand("ping", (event, args) -> {
			RequestBuffer.request(() -> event.getChannel().sendMessage("Pong! :ping_pong:"));
		});
	}
	
	private void unregisterCommands() {
		COMMAND_MAP.clear();
	}
	
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!isEnabled)
			return;
		
		String[] argArray = event.getMessage().getContent().split(" ");

		if (argArray.length == 0)
			return;
		
		final String mentionPrefix = UltimateGDBot.client().getOurUser().mention() + " ";
		String prefixUsed = "";
		
		if (argArray[0].startsWith(UltimateGDBot.property("ultimategdbot.prefix.full")))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.full");
		else if (argArray[0].startsWith(UltimateGDBot.property("ultimategdbot.prefix.canonical")))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.canonical");
		else if (argArray[0].startsWith(mentionPrefix)) {
			prefixUsed = mentionPrefix;
		} else {
			return;
		}

		final String cmdName = argArray[0].substring(prefixUsed.length()).toLowerCase();
		final List<String> args = new ArrayList<>(Arrays.asList(argArray));
		args.remove(0);
		
		new Thread(() -> {
			try {
				if (COMMAND_MAP.containsKey(cmdName)) {
					Command cmd = COMMAND_MAP.get(cmdName);
					if (!(cmd instanceof CoreCommand) || BotRoles.isGrantedAll(event.getAuthor(), event.getChannel(),
							((CoreCommand) cmd).getRolesRequired())) {
						RequestBuffer.request(() -> event.getChannel().setTypingStatus(true));
						cmd.runCommand(event, args);
					}
					else
						throw new CommandFailedException("You don't have permission to use this command");
				}
			} catch (CommandFailedException e) {
				BotUtils.sendMessage(event.getChannel(), ":negative_squared_cross_mark: " + e.getMessage());
			} catch (DiscordException e) {
				BotUtils.sendMessage(event.getChannel(), ":negative_squared_cross_mark: Sorry, an error occured"
						+ " while running the command.\n```\n" + e.getErrorMessage() + "\n```");
				System.err.println(e.getErrorMessage());
			} catch (Exception e) {
				BotUtils.sendMessage(event.getChannel(), "An internal error occured while running the command."
						+ " Please try again later.");
				UltimateGDBot.logError(
						"An internal error occured in the command handler\n"
								+ "Context info:\n"
								+ "```\n"
								+ "Guild: " + event.getGuild().getName() + " (" + event.getGuild().getLongID() + ")\n"
								+ "Channel: #" + event.getChannel().getName() + "\n"
								+ "Author: " + event.getAuthor().getName() + "#" + event.getAuthor().getDiscriminator()
										+ "(" + event.getAuthor().getLongID() + ")\n"
								+ "Full message: " + event.getMessage().getContent() + "\n"
								+ "```\n");
				e.printStackTrace();
			} finally {
				RequestBuffer.request(() -> event.getChannel().setTypingStatus(false));
			}
		}).start();
	}

}
