package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.reply.Reply;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

/**
 * An interactive menu is a command which only role is to provide nested commands,
 * accessible via an interactive menu or directly via args.
 * 
 * @author Alex1304
 *
 */
public class InteractiveMenu implements Command {

	private Map<String, Command> subCommandMap;
	private String menuEmbedContent;
	private String menuContent;

	public InteractiveMenu() {
		this.subCommandMap = new ConcurrentHashMap<>();
		this.menuEmbedContent = "";
		this.menuContent = "";
	}

	/**
	 * Triggers a subcommand by giving its name
	 * 
	 * @param cmdName
	 *            - The name of the sub-command to trigger
	 * @param event
	 *            - the Discord event info provided by the parent core command
	 * @param args
	 *            - arguments to give to the subcommand
	 * @return false if the command could not be found or failed to execute, true otherwise
	 */
	public boolean triggerSubCommand(String cmdName, MessageReceivedEvent event, List<String> args) {
		if (subCommandMap.containsKey(cmdName.toLowerCase())) {
			CommandsModule.executeCommand(subCommandMap.get(cmdName.toLowerCase()), event, args);
			return true;
		} else
			return false;
	}
	
	@Override
	public final void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		InvalidCommandArgsException invalidArgsException = new InvalidCommandArgsException(
				this.buildInvalidSyntaxMessage(BotUtils.commandWithoutArgs(event.getMessage().getContent(), args)));
		
		if (!args.isEmpty()) { // First tries to guess subcommand in args
			if (!subCommandMap.containsKey(args.get(0).toLowerCase()))
				throw invalidArgsException;
			
			triggerSubCommand(args.get(0), event, args.subList(1, args.size()));
			return;
		}

		// If no subcommand could be resolved with args, we will ask the user
		// using an interactive menu
		try {
			ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
			
			StringBuffer menu = new StringBuffer();
			
			menu.append(menuEmbedContent);
			menu.append('\n');
			
			menu.append(String.format("**This menu will close after %s of inactivity, or type `cancel`**",
					BotUtils.formatTimeMillis(Reply.DEFAULT_TIMEOUT_MILLIS)));
			
			EmbedBuilder eb = new EmbedBuilder();
			eb.appendDescription(menu.toString());
			IMessage menuMsg = BotUtils.sendMessage(event.getChannel(), menuContent, eb.build());
			
			Reply r = new Reply(menuMsg, event.getAuthor(), message -> {
				List<String> newArgs = Arrays.asList(message.getContent().split(" "));
				return triggerSubCommand(newArgs.get(0), event, newArgs.subList(1, newArgs.size()));
			});
			
			rm.open(r, true, true);
		} catch (ModuleUnavailableException e) {
			throw invalidArgsException;	
		}
	}

	/**
	 * Adds a subcommand by giving a name and an instance of {@link Command}.
	 * 
	 * @param name - The name of the subcommand
	 * @param cmd - The subcommand instance
	 */
	public void addSubCommand(String name, Command cmd) {
		this.subCommandMap.put(name, cmd);
	}
	
	private String buildInvalidSyntaxMessage(String invalidInput) {
		StringBuffer sb = new StringBuffer();
		
		for (String sc : subCommandMap.keySet()) {
			sb.append('`');
			sb.append(invalidInput.toLowerCase());
			sb.append(" ");
			sb.append(sc);
			sb.append("`\n");
		}
		
		return sb.toString();
	}

	/**
	 * Gets the menuEmbedContent
	 *
	 * @return String
	 */
	public String getMenuEmbedContent() {
		return menuEmbedContent;
	}

	/**
	 * Sets the menuEmbedContent
	 *
	 * @param menuContent - String
	 */
	public void setMenuEmbedContent(String menuEmbedContent) {
		this.menuEmbedContent = menuEmbedContent;
	}

	/**
	 * Gets the menuContent
	 *
	 * @return String
	 */
	public String getMenuContent() {
		return menuContent;
	}

	/**
	 * Sets the menuContent
	 *
	 * @param menuContent - String
	 */
	public void setMenuContent(String menuContent) {
		this.menuContent = menuContent;
	}
}
