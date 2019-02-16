package com.github.alex1304.ultimategdbot.core.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.Utils;
import com.github.alex1304.ultimategdbot.core.impl.ContextImpl;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.scheduler.Schedulers;

/**
 * Registers and executes commands when called.
 */
public class CommandHandler implements Handler {
	
	private final Bot bot;
	private final Map<String, Command> commands;
	private final Map<Plugin, Set<Command>> commandsByPlugins;
	private final Map<Command, Map<String, Command>> subCommands;
	private final Set<Plugin> plugins;
	
	public CommandHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = new HashMap<>();
		this.commandsByPlugins = new HashMap<>();
		this.subCommands = new HashMap<>();
		this.plugins = new HashSet<>();
	}
	
	/**
	 * Listens to MessageCreateEvents to trigger commands.
	 */
	@Override
	public void listen() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class).subscribeOn(Schedulers.elastic())
				.filter(event -> event.getMessage().getContent().isPresent())
				.map(event -> new ContextImpl(event, new ArrayList<>(), bot))
				.doOnNext(ctx -> System.out.println("prefix: (" + ctx.getEffectivePrefix() + ")"))
				.filter(ctx -> Utils.removeQuotesUnlessEscaped(ctx.getEvent().getMessage().getContent().get()).startsWith(ctx.getEffectivePrefix()))
				.doOnNext(ctx -> ctx.getArgs().addAll(Utils.parseArgs(ctx.getEvent().getMessage().getContent().get(), ctx.getEffectivePrefix())))
				.subscribe(ctx -> {
					var cmdName = ctx.getArgs().get(0).substring(ctx.getEffectivePrefix().length());
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
		var parts = new LinkedList<>(Arrays.asList(name.split(" +")));
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
	
	public Set<Plugin> getPlugins() {
		return plugins;
	}

	public Map<String, Command> getCommands() {
		return commands;
	}

	public Map<Plugin, Set<Command>> getCommandsByPlugins() {
		return commandsByPlugins;
	}

	public Map<Command, Map<String, Command>> getSubCommands() {
		return subCommands;
	}
}
