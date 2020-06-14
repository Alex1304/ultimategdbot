package com.github.alex1304.ultimategdbot.api.command;

import java.util.Locale;
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
	 * @param locale the locale indicating the language of the documentation
	 * @return the documentation
	 */
	CommandDocumentation getDocumentation(Locale locale);
	
	/**
	 * Gets the name of the permission required to use the command.
	 * 
	 * @return the required permission. Empty string means no requirement
	 */
	default String getRequiredPermission() {
		return "";
	}
	
	/**
	 * Gets the permission level required to use the command.
	 * 
	 * @return the required permission level
	 */
	default PermissionLevel getMinimumPermissionLevel() {
		return PermissionLevel.PUBLIC;
	}
	
	/**
	 * Gets the scope of this command.
	 * 
	 * @return the scope
	 */
	default Scope getScope() {
		return Scope.ANYWHERE;
	}
}
