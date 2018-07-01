package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.modules.commands.impl.help.HelpCommand;
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

	private Map<String, Command> commandMap;
	
	private boolean isEnabled;

	public CommandsModule() {
		this.isEnabled = false;
		this.commandMap = new HashMap<>();
	}

	@Override
	public void start() {
		registerCommands();
		isEnabled = true;
	}

	@Override
	public void stop() {
		isEnabled = false;
		unregisterCommands();
	}

	/**
	 * Puts a command into the map, associated by name
	 * @param cmd
	 */
	public void registerCommand(String name, Command cmd) {
		commandMap.put(name, cmd);
	}
	
	/**
	 * This is where the command map is loaded with command instances
	 */
	private void registerCommands() {
		registerCommand("ping", (event, args) -> {
			BotUtils.sendMessage(event.getChannel(), "Pong! :ping_pong:");
		});
		
		registerCommand("help", new HelpCommand());
	}
	
	/**
	 * Clears the command map
	 */
	private void unregisterCommands() {
		commandMap.clear();
	}
	
	/**
	 * Executes a command
	 * 
	 * @param cmd - The command instance
	 * @param event - The message received event containing context info of the command
	 * @param args - The arguments of the command
	 */
	public void executeCommand(Command cmd, MessageReceivedEvent event, List<String> args) {
		new Thread(() -> {
			try {
				if (BotRoles.isGrantedAll(event.getAuthor(), event.getChannel(), cmd.getRolesRequired())) {
					RequestBuffer.request(() -> event.getChannel().setTypingStatus(true));
					cmd.runCommand(event, args);
				}
				else
					throw new CommandFailedException("You don't have permission to use this command");
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
	
	/**
	 * Handles the message received event from Discord and runs the command if
	 * prefix and user permissions match
	 * 
	 * @param event - Contains context of the message received
	 */
	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event) {
		if (!isEnabled)
			return;
		
		if (event.getAuthor().isBot())
			return;
		
		String[] argArray = event.getMessage().getContent().split(" ");

		if (argArray.length == 0)
			return;
		
		String prefixUsed = BotUtils.prefixUsedInMessage(argArray[0]);
		boolean isMentionPrefix = BotUtils.isMentionPrefix(argArray[0]);
		
		if (prefixUsed == null)
			return;

		final String cmdName = isMentionPrefix ?
				argArray[1] : argArray[0].substring(prefixUsed.length()).toLowerCase();
		final List<String> args = new ArrayList<>(Arrays.asList(argArray));
		
		if (isMentionPrefix)
			args.remove(0);
		args.remove(0);
		
		if (commandMap.containsKey(cmdName))
			executeCommand(commandMap.get(cmdName), event, args);
	}

	/**
	 * Gets the commandMap
	 *
	 * @return Map<String,Command>
	 */
	public Map<String, Command> getCommandMap() {
		return commandMap;
	}

}
