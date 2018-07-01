package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.reply.Reply;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

/**
 * A supercommand is a command which only role is to provide nested commands,
 * accessible either via an interactive menu or directly via args.
 * 
 * @author Alex1304
 *
 */
public abstract class SuperCommand implements Command {

	private Map<String, Command> subCommandMap;
	private List<String> menuItems;

	public SuperCommand() {
		this.subCommandMap = new ConcurrentHashMap<>();
		this.menuItems = new ArrayList<>();
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
	 * @return false if the command could not be found, true otherwise
	 */
	public boolean triggerSubCommand(String cmdName, MessageReceivedEvent event, List<String> args) {
		if (subCommandMap.containsKey(cmdName)) {
			CommandsModule cm = (CommandsModule) UltimateGDBot.getModule("commands");
			cm.executeCommand(subCommandMap.get(cmdName), event, args);
			return true;
		} else
			return false;
	}
	
	@Override
	public final void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (!args.isEmpty()) { // First tries to guess subcommand in args
			if (!triggerSubCommand(args.get(0), event, args.subList(1, args.size())))
				throw new InvalidCommandArgsException();
			return;
		}

		// If no subcommand could be resolved with args, we will ask the user
		// using an interactive menu
		ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
		
		StringBuffer menu = new StringBuffer();
		
		for (String item : menuItems) {
			menu.append(item);
			menu.append('\n');
		}
		
		menu.append(String.format("**This menu will close after %s of inactivity, or type `cancel`**",
				BotUtils.formatTimeMillis(Reply.REPLY_TIMEOUT_MILLIS)));
		
		IMessage menuMsg = BotUtils.sendMessage(event.getChannel(), menu.toString());
		
		Timer t = new Timer();
		TimerTask tsk = new TimerTask() {

			@Override
			public void run() {
				if (!menuMsg.isDeleted())
					menuMsg.delete();
			}
			
		};
		t.schedule(tsk, Reply.REPLY_TIMEOUT_MILLIS);
		
		rm.open(new Reply(event.getAuthor(), event.getChannel(), message -> {
			t.cancel();
			List<String> newArgs = Arrays.asList(message.split(" "));
			return triggerSubCommand(newArgs.get(0), event, newArgs.subList(1, newArgs.size()));
		}, () -> tsk.run()));
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

	/**
	 * Adds an item in the interactive menu
	 * 
	 * @param menuItem - the item to add
	 */
	public void addMenuItem(String menuItem) {
		this.menuItems.add(menuItem);
	}
}
