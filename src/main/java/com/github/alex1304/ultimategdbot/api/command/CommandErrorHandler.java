package com.github.alex1304.ultimategdbot.api.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.database.DatabaseException;
import com.github.alex1304.ultimategdbot.api.util.Markdown;

import discord4j.rest.http.client.ClientException;
import discord4j.rest.request.DiscardedRequestException;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * Provides a convenient way to add error handlers for bot commands.
 */
public final class CommandErrorHandler {
	private static final Logger LOGGER = Loggers.getLogger(CommandErrorHandler.class);
	
	private final Map<Class<? extends Throwable>, BiFunction<Throwable, Context, Mono<Void>>> handlers = new LinkedHashMap<>();
	
	public CommandErrorHandler() {
		initDefaultHandlers();
	}
	
	/**
	 * Adds an error handler.
	 * 
	 * @param <T> the type of error to handle
	 * @param errorClass the type of error to handler
	 * @param handleAction the action to execute according to the error instance and the context
	 */
	@SuppressWarnings("unchecked")
	public <T extends Throwable> void addHandler(Class<T> errorClass, BiFunction<T, Context, Mono<Void>> handleAction) {
		handlers.put(errorClass, (error, ctx) -> handleAction.apply((T) error, ctx));
	}
	
	/**
	 * Applies the handler on the resulting Mono of {@link Command#run(Context)}.
	 * 
	 * @param commandMono the Mono returned by {@link Command#run(Context)}
	 * @param ctx the context in which the command was used
	 * @return a new Mono&lt;Void&gt; identical to the given commandMono but with the error handlers applied.
	 */
	public Mono<Void> apply(Mono<Void> commandMono, Context ctx) {
		for (var handler : handlers.entrySet()) {
			commandMono = commandMono.onErrorResume(handler.getKey(), e -> handler.getValue().apply(e, ctx));
		}
		return commandMono;
	}
	
	private void initDefaultHandlers() {
		addHandler(CommandFailedException.class, (e, ctx) -> ctx.reply(":no_entry_sign: " + e.getMessage()).then());
//		addHandler(InvalidSyntaxException.class, (e, ctx) -> ctx.reply(":no_entry_sign: Invalid syntax!"
//				+ "\n```\n" + ctx.getPrefixUsed() + ctx.getArgs().get(0) + " " + ctx.getCommand().getSyntax()
//				+ "\n```\n" + "See `" + ctx.getPrefixUsed() + "help " + ctx.getArgs().get(0) + "` for more information.").then());
		addHandler(PermissionDeniedException.class, (e, ctx) -> ctx.reply(ctx.translate("CommonStrings", "command_perm_denied")).then());
		addHandler(ClientException.class, (e, ctx) -> {
			LOGGER.debug("Discord ClientException thrown when using a command. User input: "
					+ ctx.event().getMessage().getContent() + ", Error:", e);
			var responseOptional = e.getErrorResponse();
			var message = responseOptional.map(response -> {
				var sb = new StringBuilder();
				response.getFields().forEach((k, v) -> sb.append(k).append(": ").append(String.valueOf(v)).append("\n"));
				return ctx.translate("CommonStrings", "command_discord_error") + "\n"
						+ Markdown.code(e.getStatus().code() + " " + e.getStatus().reasonPhrase()) + "\n"
						+ Markdown.codeBlock(sb.toString())
						+ (e.getStatus().code() == 403 ? ctx.translate("CommonStrings", "command_ensure_perms") : "");
			}).orElse(ctx.translate("CommonStrings", "command_discord_error"));
			
			return ctx.reply(message).then();
		});
		addHandler(DatabaseException.class, (e, ctx) -> Mono.when(
				ctx.reply(ctx.translate("CommonStrings", "command_database_access_error")),
				Mono.fromRunnable(() -> LOGGER.error("A database error occured", e))));
		addHandler(DiscardedRequestException.class, (e, ctx) -> Mono.fromRunnable(() -> LOGGER.warn(e.toString())));
	}
	
	@Override
	public String toString() {
		return "CommandErrorHandler{handledErrors=[" + handlers.keySet().stream()
				.map(Class::getName).collect(Collectors.joining(", ")) + "]}";
	}
}
