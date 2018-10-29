package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Map;

import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Provides methods to handle and execute commands
 * 
 * @author alex1304
 *
 */
public final class CommandExecutor {

	private CommandExecutor() {
	}

	/**
	 * Executes a command asynchronously. It returns a Mono emitting the
	 * {@code MessageCreateSpec} returned by the command if executed successfully,
	 * or an error otherwise.
	 * 
	 * @param cmd - Command
	 * @return Mono&lt;Boolean&gt;
	 * @throws IllegalStateException if the command handler hasn't been defined
	 */
	public static Mono<MessageCreateSpec> execute(Command cmd, DiscordContext ctx) {
		return Mono.create(sink -> {
			try {
				sink.success(cmd.execute(ctx));
			} catch (CommandFailedException | RuntimeException e) {
				sink.error(e);
			}
		});
	}

	/**
	 * Allows to execute subcommands. {@code ctx} contains the context of the parent
	 * command. The first argument from {@link DiscordContext#getArgs()} is read,
	 * and looks for an entry in the given {@code subcommandMap} that matches the
	 * read value. The command associated to this entry is then executed
	 * <b>synchronously</b> with a copy of {@code ctx} minus the first argument.
	 * 
	 * @param subcommandMap - Map&lt;String, Command&gt;
	 * @param ctx           - DiscordContext
	 * @return the MessageCreateSpec returned by the subcommand if successfully
	 *         executed
	 * @throws CommandFailedException forwards an eventual CommandFailedException
	 *                                thrown by the subcommand
	 */
	public static MessageCreateSpec executeSubcommand(Map<String, Command> subcommandMap, DiscordContext ctx)
			throws CommandFailedException {
		var args = ctx.getArgs();
		if (args.isEmpty()) {
			throw new CommandFailedException(
					"Missing argument(s). Use `" + UltimateGDBot.getInstance().getCanonicalPrefix()
							+ "help <commandName>` to see syntax reference and usage examples");
		} else if (!subcommandMap.containsKey(args.get(0))) {
			throw new CommandFailedException("Unrecognized argument \"" + args.get(0) + "\". Use `"
					+ UltimateGDBot.getInstance().getCanonicalPrefix()
					+ "help <commandName>` to see syntax reference and usage examples");
		}

		return subcommandMap.get(args.get(0)).execute(new DiscordContext(ctx.getEvent(), args.subList(1, args.size())));
	}
}
