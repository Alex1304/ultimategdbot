package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.modules.reply.Reply;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;
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
		commandMap.put(cmd.getName(), cmd);
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
			RequestBuffer.request(() -> event.getChannel().sendMessage("Pong! :ping_pong:"));
		});
		
		registerCommand("reply", (event, args) -> {
			BotUtils.sendMessage(event.getChannel(), "Hello");
			ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
			rm.open(new Reply(event.getAuthor(), event.getChannel(), message -> {
				if (message.equalsIgnoreCase("hello")) {
					BotUtils.sendMessage(event.getChannel(), "How are you?");
					rm.open(new Reply(event.getAuthor(), event.getChannel(), message2 -> {
						if (message2.equalsIgnoreCase("fine"))
							BotUtils.sendMessage(event.getChannel(), "Great!");
						else if (message2.equalsIgnoreCase("bad"))
							BotUtils.sendMessage(event.getChannel(), "Oh, sad to hear that...");
						else
							BotUtils.sendMessage(event.getChannel(), "Ok");
					}));
				} else if (message.equalsIgnoreCase("gay")) {
					BotUtils.sendMessage(event.getChannel(), "no u");
					rm.open(new Reply(event.getAuthor(), event.getChannel(), message2 -> {
						if (message2.equalsIgnoreCase("no u"))
							BotUtils.sendMessage(event.getChannel(), "Damn reverse card! You got me.");
					}));
				}
			}));
		});
	}
	
	/**
	 * Clears the command map
	 */
	private void unregisterCommands() {
		commandMap.clear();
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
		
		final String mentionPrefix = UltimateGDBot.client().getOurUser().mention(true);
		final String mentionPrefix2 = UltimateGDBot.client().getOurUser().mention(false);
		String prefixUsed = "";
		
		if (argArray[0].startsWith(UltimateGDBot.property("ultimategdbot.prefix.full")))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.full");
		else if (argArray[0].startsWith(UltimateGDBot.property("ultimategdbot.prefix.canonical")))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.canonical");
		else if (argArray[0].equals(mentionPrefix))
			prefixUsed = mentionPrefix;
		else if (argArray[0].equals(mentionPrefix2))
			prefixUsed = mentionPrefix2;
		else {
			return;
		}

		final String cmdName = (prefixUsed.equals(mentionPrefix) || prefixUsed.equals(mentionPrefix2) ?
				argArray[1] : argArray[0].substring(prefixUsed.length())).toLowerCase();
		final List<String> args = new ArrayList<>(Arrays.asList(argArray));
		
		if (prefixUsed.equals(mentionPrefix) || prefixUsed.equals(mentionPrefix2))
			args.remove(0);
		args.remove(0);
		
		new Thread(() -> {
			try {
				if (commandMap.containsKey(cmdName)) {
					Command cmd = commandMap.get(cmdName);
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

	/**
	 * Gets the commandMap
	 *
	 * @return Map<String,Command>
	 */
	public Map<String, Command> getCommandMap() {
		return commandMap;
	}

}
