package com.github.alex1304.ultimategdbot.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * The kernel for all bot commands. Command instances are stored, managed
 * and executed here.
 */
public interface CommandKernel {
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
	 * Invokes a command with the specified context.
	 * 
	 * @param cmd the command to invoke
	 * @param ctx the context of the command
	 * @return a Mono that completes when the command has terminated. Any errors
	 *         that may occur when running the command are transmitted through this
	 *         Mono.
	 */
	Mono<Void> invokeCommand(Command cmd, Context ctx);
}
