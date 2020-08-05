package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Objects.requireNonNull;

import reactor.core.publisher.Mono;

/**
 * Represents a command ready to be executed, meaning that the context and the
 * error handler of the command are already defined.
 */
public final class ExecutableCommand {

	private final Command command;
	private final Context context;
	private final CommandErrorHandler errorHandler;
	private final PermissionChecker permissionChecker;
	
	public ExecutableCommand(Command command, Context context, CommandErrorHandler errorHandler, PermissionChecker permissionChecker) {
		this.command = requireNonNull(command);
		this.context = requireNonNull(context);
		this.errorHandler = requireNonNull(errorHandler);
		this.permissionChecker = requireNonNull(permissionChecker);
	}
	
	/**
	 * Executes the command by taking into account the context and by applying the
	 * error handler. Scope and permission checks are performed here. If the user is
	 * not granted the permission to use the command, the command will fail with a
	 * {@link PermissionDeniedException}. If the context is outside the scope of the
	 * command, nothing happens and the command completes empty.
	 * 
	 * @return a Mono completing when the command execution is complete. Errors
	 *         caused by a failed permission check or an abnormal termination of the
	 *         command will be forwarded through this Mono.
	 */
	public Mono<Void> execute() {
		if (!command.getScope().isInScope(context.channel())) {
			return Mono.empty();
		}
		return errorHandler.apply(checkPermission()
				.and(checkPermissionLevel())
				.then(command.run(context)), context);
	}
	
	private Mono<?> checkPermission() {
		return Mono.just(command.getRequiredPermission())
				.filterWhen(perm -> permissionChecker.isGranted(perm, context))
				.switchIfEmpty(Mono.error(new PermissionDeniedException()));
	}
	
	private Mono<?> checkPermissionLevel() {
		return Mono.just(command.getMinimumPermissionLevel())
				.filterWhen(perm -> permissionChecker.isGranted(perm, context))
				.switchIfEmpty(Mono.error(new PermissionDeniedException()));
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

	@Override
	public String toString() {
		return "ExecutableCommand{command=" + command + ", context=" + context + "}";
	}
}
