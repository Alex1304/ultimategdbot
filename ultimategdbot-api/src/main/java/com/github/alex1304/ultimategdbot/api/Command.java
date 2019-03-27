package com.github.alex1304.ultimategdbot.api;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

/**
 * Represents a bot command.
 */
public interface Command {
	/**
	 * Executes the command
	 * 
	 * @param ctx the context
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
	 * Gets the description of the command. The description is shown in the help
	 * command list.
	 * 
	 * @return the description
	 */
	String getDescription();

	/**
	 * Gets the long description of the command. The description is shown in the
	 * detailed help page of the command.
	 * 
	 * @return the description
	 */
	String getLongDescription();

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

	/**
	 * Creates a new Command instance which action is the provided function, and
	 * other attributes (alias, error actions, allowed channels, permission level,
	 * etc) are inherited from the given base command.
	 * 
	 * @param other       the base command
	 * @param executeFunc the action of the new command
	 * @return a new Command
	 */
	static Command forkedFrom(Command other, Function<Context, Mono<Void>> executeFunc) {
		return new Command() {
			@Override
			public Mono<Void> execute(Context ctx0) {
				return executeFunc.apply(ctx0);
			}

			@Override
			public Set<String> getAliases() {
				return other.getAliases();
			}

			@Override
			public Set<Command> getSubcommands() {
				return other.getSubcommands();
			}

			@Override
			public String getDescription() {
				return other.getDescription();
			}

			@Override
			public String getLongDescription() {
				return other.getLongDescription();
			}

			@Override
			public String getSyntax() {
				return other.getSyntax();
			}

			@Override
			public PermissionLevel getPermissionLevel() {
				return other.getPermissionLevel();
			}

			@Override
			public EnumSet<Type> getChannelTypesAllowed() {
				return other.getChannelTypesAllowed();
			}

			@Override
			public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
				return other.getErrorActions();
			}
		};
	}
}
