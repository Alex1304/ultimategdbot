package com.github.alex1304.ultimategdbot.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;
import discord4j.rest.http.client.ClientException;
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
	
	/**
	 * Executes the command with the supplied context.
	 * 
	 * @param cmd - the command to invoke
	 * @param ctx - the context to apply
	 */
	static void invoke(Command cmd, Context ctx) {
		ctx.getEvent().getMessage().getChannel()
				.filter(c -> cmd.getChannelTypesAllowed().contains(c.getType()))
				.flatMap(c -> cmd.getPermissionLevel().isGranted(ctx))
				.flatMap(isGranted -> isGranted ? cmd.execute(ctx) : Mono.error(new CommandPermissionDeniedException()))
				.doOnError(error -> {
					var actions = cmd.getErrorActions();
					if (error instanceof CommandFailedException) {
						actions.getOrDefault(CommandFailedException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: " + e.getMessage()).subscribe();
						}).accept(error, ctx);
					} else if (error instanceof CommandPermissionDeniedException) {
						actions.getOrDefault(CommandFailedException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: You don't have the required permissions to run this command.").subscribe();
						}).accept(error, ctx);
					} else if (error instanceof InvalidSyntaxException) {
						actions.getOrDefault(InvalidSyntaxException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: Invalid syntax.\n" + ctx0.getBot().getCommandDoc(cmd, ctx0.getEffectivePrefix())).subscribe();
						}).accept(error, ctx);
					} else if (error instanceof ClientException) {
						actions.getOrDefault(ClientException.class, (e, ctx0) -> {
							var ce = (ClientException) e;
							var h = ce.getErrorResponse();
							var sj = new StringJoiner("", "```\n", "```\n");
							h.getFields().forEach((k, v) -> sj.add(k).add(": ").add(String.valueOf(v)).add("\n"));
							ctx0.reply(":no_entry_sign: Discord returned an error when executing this command: "
									+ "`" + ce.getStatus().code() + " " + ce.getStatus().reasonPhrase() + "`\n"
									+ sj.toString()
									+ "Make sure that I have sufficient permissions in this server and try again.")
							.subscribe();
						}).accept(error, ctx);
					} else {
						actions.getOrDefault(error.getClass(), (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: An internal error occured. A crash report has been sent to the developer. Sorry for the inconvenience.")
									.subscribe();
							e.printStackTrace();
						}).accept(error, ctx);
					}
				}).subscribe();
	}
	
	/**
	 * Executes a function that is treated as a command. 
	 * 
	 * @param executeFunc - the function to execute
	 * @param ctx - the context to apply
	 */
	static void invoke(Function<Context, Mono<Void>> executeFunc, Context ctx) {
		invoke(new Command() {
			@Override
			public Mono<Void> execute(Context ctx0) {
				return executeFunc.apply(ctx0);
			}

			@Override
			public Set<String> getAliases() {
				return Collections.emptySet();
			}

			@Override
			public Set<Command> getSubcommands() {
				return Collections.emptySet();
			}

			@Override
			public String getDescription() {
				return "";
			}

			@Override
			public String getSyntax() {
				return "";
			}

			@Override
			public PermissionLevel getPermissionLevel() {
				return PermissionLevel.PUBLIC;
			}

			@Override
			public EnumSet<Type> getChannelTypesAllowed() {
				return EnumSet.of(Type.GUILD_TEXT, Type.DM);
			}

			@Override
			public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
				return Collections.emptyMap();
			}
		}, ctx);
	}
}
