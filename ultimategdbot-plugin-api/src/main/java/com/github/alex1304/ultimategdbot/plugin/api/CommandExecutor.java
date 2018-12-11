package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Map;
import java.util.Objects;

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
	 * Allows to execute subcommands. {@code ctx} contains the context of the parent
	 * command. The first argument from {@link DiscordContext#getArgs()} is read,
	 * and looks for an entry in the given {@code subcommandMap} that matches the
	 * read value. The command associated to this entry is then executed
	 * <b>synchronously</b> with a copy of {@code ctx} minus the first argument. If
	 * the subcommand could not be read from arguments, an alternative command
	 * called onArgumentMissing will be executed instead. If no subcommand matches
	 * the argument, an alternative command called onSubcommandNotFound will be
	 * executed instead.
	 * 
	 * @param subcommandMap        - Map&lt;String, Command&gt;
	 * @param ctx                  - DiscordContext
	 * @param onArgumentMissing    - Command
	 * @param onSubcommandNotFound - Command
	 * @throws CommandFailedException forwards an eventual CommandFailedException
	 *                                thrown by the subcommand, by onArgumentMissing
	 *                                or by onSubcommandNotFound
	 */
	public static Mono<Void> executeSubcommand(Map<String, Command> subcommandMap, DiscordContext ctx,
			Command onArgumentMissing, Command onSubcommandNotFound) {
		Objects.requireNonNull(subcommandMap);
		Objects.requireNonNull(ctx);
		Objects.requireNonNull(onArgumentMissing);
		Objects.requireNonNull(onSubcommandNotFound);

		var args = ctx.getArgs();
		if (args.isEmpty()) {
			onArgumentMissing.execute(ctx);
		} else if (!subcommandMap.containsKey(args.get(0))) {
			onSubcommandNotFound.execute(ctx);
		}

		return subcommandMap.get(args.get(0)).execute(new DiscordContext(ctx, args.subList(1, args.size())));
	}

	/**
	 * Allows to execute subcommands. {@code ctx} contains the context of the parent
	 * command. The first argument from {@link DiscordContext#getArgs()} is read,
	 * and looks for an entry in the given {@code subcommandMap} that matches the
	 * read value. The command associated to this entry is then executed
	 * <b>synchronously</b> with a copy of {@code ctx} minus the first argument. If
	 * the subcommand could not be read from arguments, or if no subcommand matches
	 * the argument, then this method will do nothing.
	 * 
	 * @param subcommandMap - Map&lt;String, Command&gt;
	 * @param ctx           - DiscordContext
	 * @throws CommandFailedException forwards an eventual CommandFailedException
	 *                                thrown by the subcommand
	 */
	public static Mono<Void> executeSubcommand(Map<String, Command> subcommandMap, DiscordContext ctx) {
		return executeSubcommand(subcommandMap, ctx, Command.DO_NOTHING, Command.DO_NOTHING);
	}
}
