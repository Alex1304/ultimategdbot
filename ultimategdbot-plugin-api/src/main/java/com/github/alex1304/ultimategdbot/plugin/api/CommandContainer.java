package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Iterator;
import java.util.Objects;
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
	 * Adds all items into this container.
	 * 
	 * @param commands - ServiceLoader
	 */
	public void addAll(Iterator<Command> commands) {
		Objects.requireNonNull(commands);
		while (commands.hasNext()) {
			var cmd = commands.next();
			commandMap.put(cmd.getName(), cmd);
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
