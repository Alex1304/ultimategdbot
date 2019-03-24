package com.github.alex1304.ultimategdbot.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * The central point for all bot commands. Command instances are stored, managed
 * and executed here.
 */
public interface CommandKernel {
	/**
	 * Starts the kernel.
	 */
	void start();
	
	/**
	 * Reads the command line and retrieves the corresponding Command instance.
	 * Arguments passed to the command are also returned as a list.
	 * 
	 * @param commandLine the command line
	 * 
	 * @return the command with its arguments, if present
	 */
	Optional<Tuple2<Command, List<String>>> parseCommandLine(String commandLine);
	
	/**
	 * Reads the command line and retrieves the corresponding Command instance.
	 * Arguments passed to the command are also returned as a list.
	 * 
	 * @param commandLine the command line as a list of string containing the
	 *                    command name as first element and arguments then
	 * 
	 * @return the command with its arguments, if present
	 */
	Optional<Tuple2<Command, List<String>>> parseCommandLine(List<String> commandLine);
	
	/**
	 * Gets an unmodifiable set of all commands available in the kernel.
	 * 
	 * @return a Set of Command
	 */
	Set<Command> getCommands();
	
	/**
	 * Gets an unmodifiable map of all commands available in the kernel, grouped by plugin names.
	 * 
	 * @return a Map of String, Command
	 */
	Map<String, Set<Command>> getCommandsGroupedByPlugins();
	
	/**
	 * Invokes a command with the specified context.
	 * 
	 * @param cmd the command to invoke
	 * @param ctx the context of the command
	 * @return a Mono that completes when the command has terminated. Any errors
	 *         that may occur when running the command are transmitted through this
	 *         Mono.
	 */
	Mono<Void> invokeCommand(Command cmd, Context ctx);

	/**
	 * Opens a new reply menu with the given items.
	 * 
	 * @param ctx             the context of the command this reply menu was opened
	 *                        from
	 * @param msg             The message containing the menu
	 * @param menuItems       the menu items
	 * @param deleteOnReply   Whether to delete {@code msg} when the user has given
	 *                        a valid reply
	 * @param deleteOnTimeout Whether to delete {@code msg} when the user doesn't
	 *                        reply and the menu times out.
	 * @return the identifier of the opened reply, or an empty string if the menu
	 *         could not be opened.
	 */
	String openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems,
			boolean deleteOnReply, boolean deleteOnTimeout);

	/**
	 * Closes a reply menu using its identifier.
	 * 
	 * @param identifier the identifier of the reply menu to close
	 */
	void closeReplyMenu(String identifier);
}
