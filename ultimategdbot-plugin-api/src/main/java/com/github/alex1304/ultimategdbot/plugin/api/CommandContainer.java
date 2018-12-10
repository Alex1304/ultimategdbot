package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Loaded commands are stored here.
 * 
 * @author Alex1304
 *
 */
public class CommandContainer implements Iterable<Command> {
	
	private static CommandContainer commands = null;

	private final ConcurrentHashMap<String, Command> commandMap;
	
	private CommandContainer() {
		this.commandMap = new ConcurrentHashMap<>();
	}

	/**
	 * Gets the instance of a command by its name. Returns null if none was found.
	 * This does not check whether the command is enabled or disabled
	 * 
	 * @param commandName - String
	 * @return T
	 */
	public final Command get(String commandName) {
		return commandMap.get(Objects.requireNonNull(commandName));
	}

	/**
	 * Checks whether the given command name refers to an existing loaded command.
	 * Returns {@code true} if so, {@code false} otherwise.
	 * 
	 * @param commandName - String
	 * @return boolean
	 */
	public final boolean exists(String commandName) {
		return commandMap.contains(Objects.requireNonNull(commandName));
	}

	/**
	 * Synchronizes the items of this container to match the items loaded by the
	 * given ServiceLoader.
	 * 
	 * @param loader - ServiceLoader
	 */
	public void syncFromLoader(ServiceLoader<Command> loader) {
		commandMap.clear();
		for (var plugin : loader) {
			commandMap.put(plugin.getName(), plugin);
		}
	}
	
	/**
	 * Returns a unique instance of the plugin container for commands
	 * 
	 * @return PluginContainer&lt;Command&gt;
	 */
	public static synchronized CommandContainer getInstance() {
		if (commands == null) {
			commands = new CommandContainer();
		}
		
		return commands;
	}
	
	@Override
	public Iterator<Command> iterator() {
		return commandMap.values().iterator();
	}
	
	public Stream<Command> stream() {
		return commandMap.values().stream();
	}
}
