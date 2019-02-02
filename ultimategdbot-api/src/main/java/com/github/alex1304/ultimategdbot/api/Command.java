package com.github.alex1304.ultimategdbot.api;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import discord4j.core.object.entity.Channel;
import reactor.core.publisher.Mono;

/**
 * Represents a bot command.
 */
public interface Command {
	/**
	 * Executes the command
	 * 
	 * @param ctx - the context
	 * @return a Mono that completes empty when the command is successful, and emits
	 *         an error when something goes wrong.
	 */
	Mono<Void> execute(Context ctx);

	/**
	 * Gets the aliases for this command.
	 * 
	 * @return the set of aliases
	 */
	Set<String> getAliases();

	/**
	 * Gets the subcommands.
	 * 
	 * @return the set of subcommands
	 */
	Set<Command> getSubcommands();
	
	/**
	 * Gets the category name of the command.
	 * 
	 * @return the category name
	 */
	String getCategoryName();

	/**
	 * Gets the description of the command. The description is shown in the help
	 * command.
	 * 
	 * @return the description
	 */
	String getDescription();

	/**
	 * Gets the syntax of the command. The syntax is shown in the help command, and
	 * explains which arguments can be passed to the command.
	 * 
	 * @return the syntax
	 */
	String getSyntax();

	/**
	 * Gets the permission level required to execute this command.
	 * 
	 * @return the permission level
	 */
	PermissionLevel getPermissionLevel();

	/**
	 * Gets the type of channels this command is allowed for use in.
	 * 
	 * @return the set of allowed type of channels
	 */
	EnumSet<Channel.Type> getChannelTypesAllowed();

	/**
	 * Allows to define an action to execute according to the type of error emitted
	 * when executing this command.
	 * 
	 * @return a Map that associates a Throwable type to an executable action.
	 */
	Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions();
}
