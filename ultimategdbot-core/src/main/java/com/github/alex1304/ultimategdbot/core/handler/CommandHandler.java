package com.github.alex1304.ultimategdbot.core.handler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;

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
	private final Map<Command, String> commandDocs;
	
	public CommandHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = new HashMap<>();
		this.subCommands = new HashMap<>();
		this.commandDocs = new HashMap<>();
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
						commandDocs.put(element, generateDoc(element));
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
	 * Gets a set of available commands.
	 * 
	 * @return the available commands
	 */
	public Map<Command, String> getCommandDocs() {
		return commandDocs;
	}
	
	/**
	 * Gets a command instance by one of its alias
	 * 
	 * @param name - the name of the command
	 * @return the command instance
	 */
	public Command getCommandByName(String name) {
		return commands.get(name);
	}
	
	private static String generateDoc(Command cmd) {
		var prefix = "$(prefix)";
		var aliases = joinAliases(cmd.getAliases());
		var shortestAlias = shortestAlias(cmd.getAliases());
		var sb = new StringBuilder();
		sb.append("```diff\n");
		sb.append(prefix);
		sb.append(aliases);
		sb.append(' ');
		sb.append(cmd.getSyntax());
		sb.append("\n```\n");
		sb.append(cmd.getDescription());
		if (!cmd.getSubcommands().isEmpty()) {
			sb.append("\n\n**Subcommands:**\n```\n");
			cmd.getSubcommands().forEach(scmd -> {
				sb.append(prefix);
				sb.append(shortestAlias);
				sb.append(' ');
				sb.append(joinAliases(scmd.getAliases()));
				sb.append(' ');
				sb.append(scmd.getSyntax());
				sb.append("\n\t-> ");
				sb.append(scmd.getDescription());
				sb.append("\n");
			});
			sb.append("\n```\n");
		}
		return sb.toString();
	}
	
	private static String joinAliases(Set<String> aliases) {
		if (aliases.size() == 1) {
			return aliases.stream().findAny().get();
		} else {
			var aliasJoiner = new StringJoiner("|");
			aliases.stream().sorted((a, b) -> b.length() == a.length() ? a.compareTo(b) : b.length() - a.length())
					.forEach(aliasJoiner::add);
			return aliasJoiner.toString();
		}
	}
	
	private static String shortestAlias(Set<String> aliases) {
		return aliases.stream().sorted(Comparator.comparing(String::length)).findFirst().get();
	}
}
