package com.github.alex1304.ultimategdbot.plugin.api;

import reactor.core.publisher.Mono;

/**
 * Interface that bot commands should implement. A command takes a context in
 * parameter and returns a view.
 *
 * @author Alex1304
 * 
 */
@FunctionalInterface
public interface Command {

	/**
	 * Command that does nothing
	 */
	static final Command DO_NOTHING = ctx -> {
		return Mono.empty();
	};

	/**
	 * Executes the command
	 * 
	 * @param ctx - the context
	 * @return a Mono&lt;Void&gt; that completes empty when the command is
	 *         successful, and emits an error when something goes wrong. If it's a
	 *         checked use case, the error emitted should be a
	 *         {@link CommandFailedException}.
	 */
	Mono<Void> execute(DiscordContext ctx);

	/**
	 * Gets the set of roles required by the user running the command.
	 * 
	 * @return EnumSetBotRoles
	 */
	default BotRoles getRoleRequired() {
		return BotRoles.USER;
	}

	/**
	 * Gets the name of the command. By default, it is the name of the class
	 * implementing the interface in lowercase minus the Command suffix if there is
	 * any. For example, it would return {@code ping} if the class is named
	 * {@code Ping} or {@code PingCommand}. This method shouldn't return an empty
	 * string, except if this interface is implemented via an anonymous class or a
	 * lambda expression. So, if the class is named {@code Command}, the output
	 * would be {@code Command}.
	 * 
	 * @return a String
	 */
	default String getName() {
		if (this.getClass().isAnonymousClass()) {
			return "";
		}

		final var classname = this.getClass().getSimpleName().toLowerCase();
		final var suffix = "command";

		if (!classname.equals(suffix) && classname.endsWith(suffix)) {
			return classname.substring(0, classname.length() - suffix.length());
		}

		return classname;
	}

	default void initialize() throws CommandInitializationException {
		return;
	}
}
