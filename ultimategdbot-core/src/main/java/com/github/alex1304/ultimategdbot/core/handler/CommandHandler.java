package com.github.alex1304.ultimategdbot.core.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.PluginSetupException;
import com.github.alex1304.ultimategdbot.core.impl.ContextImpl;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.scheduler.Schedulers;

/**
 * Registers and executes commands when called.
 */
public class CommandHandler implements Handler {
	
	private final Bot bot;
	private final Map<String, Command> commands;
	private final Map<Command, Map<String, Command>> subCommands;
	
	public CommandHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = new HashMap<>();
		this.subCommands = new HashMap<>();
	}
	
	/**
	 * Loads commands from plugins.
	 */
	@Override
	public void prepare() {
		var loader = ServiceLoader.load(Plugin.class);
		for (var plugin : loader) {
			try {
				System.out.printf("Loading plugin: %s...\n", plugin.getName());
				plugin.setup();
				for (var cmd : plugin.getProvidedCommands()) {
					
					for (var alias : cmd.getAliases()) {
						commands.put(alias, cmd);
					}
					// Add all subcommands
					var subCmdDeque = new ArrayDeque<Command>();
					subCmdDeque.push(cmd);
					while (!subCmdDeque.isEmpty()) {
						var element = subCmdDeque.pop();
						if (subCommands.containsKey(element)) {
							continue;
						}
						var subCmdMap = new HashMap<String, Command>();
						for (var subcmd : element.getSubcommands()) {
							for (var alias : subcmd.getAliases()) {
								subCmdMap.put(alias, subcmd);
							}
						}
						subCommands.put(element, subCmdMap);
						subCmdDeque.addAll(element.getSubcommands());
					}
					System.out.printf("\tLoaded command: %s %s\n", cmd.getClass().getName(), cmd.getAliases());
				}
			} catch (PluginSetupException e) {
				System.out.printf("WARNING: Unable to load plugin %s: %s\n", plugin.getName(), e.getMessage());
			}
		}
	}
	
	/**
	 * Listens to MessageCreateEvents to trigger commands.
	 */
	@Override
	public void listen() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class).subscribeOn(Schedulers.elastic())
				.filter(event -> event.getMessage().getContent().isPresent())
				.map(event -> new ContextImpl(event, Arrays.asList(event.getMessage().getContent().get().split(" +")), bot))
				.subscribe(ctx -> {
					var prefix = ctx.getGuildSettings() != null ? ctx.getGuildSettings().getPrefix() : bot.getDefaultPrefix();
					if (!ctx.getArgs().get(0).startsWith(prefix)) {
						return;
					}
					var cmdName = ctx.getArgs().get(0).substring(prefix.length());
					var cmd = commands.get(cmdName);
					if (cmd == null) {
						return;
					}
					ctx.getArgs().set(0, cmdName);
					var argsCpy = new ArrayList<>(ctx.getArgs());
					while (argsCpy.size() > 1) {
						var subcmd = subCommands.get(cmd).get(argsCpy.get(1));
						if (subcmd == null) {
							break;
						}
						cmd = subcmd;
						var arg1 = argsCpy.remove(0);
						var arg2 = argsCpy.remove(0);
						argsCpy.add(0, String.join(" ", arg1, arg2));
					}
					var newCtx = ctx.getArgs().equals(argsCpy) ? ctx : new ContextImpl(ctx.getEvent(), argsCpy, ctx.getBot());
					Command.invoke(cmd, newCtx);
				});
	}
	
	/**
	 * Gets a command instance by one of its alias
	 * 
	 * @param name - the name of the command
	 * @return the command instance
	 */
	public Command getCommandForName(String name) {
		var parts = new ArrayList<>(Arrays.asList(name.split(" +")));
		var cmd = commands.get(parts.get(0));
		if (cmd == null) {
			return null;
		}
		while (parts.size() > 1) {
			var subcmd = subCommands.get(cmd).get(parts.get(1));
			if (subcmd == null) {
				break;
			}
			cmd = subcmd;
			var arg1 = parts.remove(0);
			var arg2 = parts.remove(0);
			parts.add(0, String.join(" ", arg1, arg2));
		}
		return cmd;
	}
}
