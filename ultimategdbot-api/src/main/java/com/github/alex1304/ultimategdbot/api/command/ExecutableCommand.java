package com.github.alex1304.ultimategdbot.api.command;

import java.util.Objects;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

public class ExecutableCommand {

	private final Command command;
	private final Context context;
	private final CommandErrorHandler errorHandler;
	
	public ExecutableCommand(Command command, Context context, CommandErrorHandler errorHandler) {
		this.command = Objects.requireNonNull(command);
		this.context = Objects.requireNonNull(context);
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	
	public Mono<Void> execute() {
		var commandMono = context.getEvent().getMessage().getChannel()
				.filter(command.getScope()::isInScope)
				.zipWhen(c -> command.getPermissionLevel().isGranted(context))
				.flatMap(TupleUtils.function((channel, isGranted) -> isGranted
						? channel.typeUntil(command.run(context)).then()
						: Mono.error(new PermissionDeniedException())));
		return errorHandler.apply(commandMono, context);
	}
	
}
