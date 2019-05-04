package com.github.alex1304.ultimategdbot.api;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import reactor.core.publisher.Mono;

/**
 * Provides a convenient way to add error handlers for bot commands.
 */
public class CommandErrorHandler {
	
	private final Map<Class<? extends Throwable>, BiFunction<Throwable, Context, Mono<Void>>> handlers = new HashMap<>();
	
	/**
	 * Adds an error handler.
	 * 
	 * @param errorClass the type of error to handler
	 * @param handleAction the action to execute according to the error instance and the context
	 */
	@SuppressWarnings("unchecked")
	public <T extends Throwable> void addHandler(Class<T> errorClass, BiFunction<T, Context, Mono<Void>> handleAction) {
		handlers.put(errorClass, (error, ctx) -> handleAction.apply((T) error, ctx));
	}
	
	/**
	 * Applies the handler on the resulting Mono of {@link Command#execute(Context)}.
	 * 
	 * @param commandMono the Mono returned by {@link Command#execute(Context)}
	 * @param ctx the context in which the command was used
	 * @return a new Mono&lt;Void&gt; identical to the given commandMono but with the error handlers applied.
	 */
	public Mono<Void> apply(Mono<Void> commandMono, Context ctx) {
		for (var handler : handlers.entrySet()) {
			commandMono = commandMono.onErrorResume(handler.getKey(), e -> handler.getValue().apply(e, ctx).onErrorResume(__ -> Mono.empty()));
		}
		return commandMono;
	}
}
