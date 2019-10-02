package com.github.alex1304.ultimategdbot.api.command;

import java.util.Objects;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

/**
 * Represents a command ready to be executed, meaning that the context and the
 * error handler of the command are already defined.
 */
public class ExecutableCommand {

	private final Command command;
	private final Context context;
	private final CommandErrorHandler errorHandler;
	
	public ExecutableCommand(Command command, Context context, CommandErrorHandler errorHandler) {
		this.command = Objects.requireNonNull(command);
		this.context = Objects.requireNonNull(context);
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	
	/**
	 * Executes the command by taking into account the context and by applying the
	 * error handler. Scope and permission checks are performed here. If the command
	 * is not in scope, the execution completes immediately without any side effect.
	 * If the user is not granted the permission to use the command, the command
	 * will fail with a {@link PermissionDeniedException}.
	 * 
	 * @return a Mono completing when the command execution is complete. Errors
	 *         caused by a failed permission check or an abnormal termination of the
	 *         command will be forwarded through this Mono.
	 */
	public Mono<Void> execute() {
		var commandMono = context.getEvent().getMessage().getChannel()
				.filter(command.getScope()::isInScope)
				.flatMap(c -> command.getPermissionLevel().checkGranted(context)
						.then(command.run(context)));
		return errorHandler.apply(commandMono, context);
	}
	
	/**
	 * Gets the underlying command instance.
	 * 
	 * @return the command
	 */
	public Command getCommand() {
		return command;
	}
	
	/**
	 * Gets the context of the command.
	 * 
	 * @return the context
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * Gets the error handler applied to this command.
	 * 
	 * @return the error handler
	 */
	public CommandErrorHandler getErrorHandler() {
		return errorHandler;
	}
	
}
