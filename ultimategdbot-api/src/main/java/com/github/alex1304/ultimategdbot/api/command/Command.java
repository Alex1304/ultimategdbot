package com.github.alex1304.ultimategdbot.api.command;

import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * Represents a bot command.
 */
public interface Command {
	/**
	 * Defines the action of the command
	 * 
	 * @param ctx the context
	 * @return a Mono that completes empty when the command is successful, and emits
	 *         an error when something goes wrong.
	 */
	Mono<Void> run(Context ctx);

	/**
	 * Gets the aliases for this command.
	 * 
	 * @return the set of aliases
	 */
	Set<String> getAliases();
	
	/**
	 * Gets the documentation of the command.
	 * 
	 * @return the documentation
	 */
	CommandDocumentation getDocumentation();

	/**
	 * Gets the permission level required to execute this command.
	 * 
	 * @return the permission level
	 */
	default PermissionLevel getPermissionLevel() {
		return PermissionLevel.PUBLIC;
	}

	/**
	 * Gets the type of channels this command is allowed for use in.
	 * 
	 * @return the set of allowed type of channels
	 */
	default Scope getScope() {
		return Scope.ANYWHERE;
	}
}
