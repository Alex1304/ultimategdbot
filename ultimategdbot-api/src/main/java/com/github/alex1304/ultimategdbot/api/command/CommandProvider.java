package com.github.alex1304.ultimategdbot.api.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.utils.InputTokenizer;

import discord4j.core.event.domain.message.MessageCreateEvent;

/**
 * Provides a set of commands. Each command handler provides their own way to
 * handle errors via a {@link CommandErrorHandler}.
 */
public class CommandProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandProvider.class);

	private CommandErrorHandler errorHandler = new CommandErrorHandler();
	private final Map<String, Command> commandMap = new HashMap<>();
	
	/**
	 * Allows to supply a custom command handler. If this method is not called, it will use a default handler.
	 * 
	 * @param errorHandler the error handler to set
	 */
	public void setErrorHandler(CommandErrorHandler errorHandler) {
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	
	/**
	 * Adds a command to this provider.
	 * 
	 * @param command the command to add
	 */
	public void add(Command command) {
		for (var alias : command.getAliases()) {
			commandMap.put(alias, command);
		}
	}
	
	/**
	 * Provides a command based on a MessageCreateEvent. The event must come with a
	 * message body containing a prefix and the alias of one of the commands
	 * provided by this provider. If it matches with a provided command, arguments
	 * and flags are parsed, and everything is wrapped in an
	 * {@link ExecutableCommand} which is returned. If the event does not match with
	 * any command, an empty {@link Optional} is returned.
	 * 
	 * @param bot the bot instance
	 * @param prefix 
	 * @param event
	 * @return
	 */
	public Optional<ExecutableCommand> provideFromEvent(Bot bot, String prefix, MessageCreateEvent event) {
		var botId = bot.getMainDiscordClient().getSelfId();
		var prefixes = Set.of("<@" + botId + ">", "<@!" + botId + ">", prefix);
		var content = event.getMessage().getContent().orElse("");
		String prefixUsed = null;
		for (var p : prefixes) {
			if (content.toLowerCase().startsWith(p.toLowerCase())) {
				content = content.substring(p.length());
				prefixUsed = p;
				break;
			}
		}
		if (prefixUsed == null) {
			LOGGER.debug("Message doesn't match any prefix");
			return Optional.empty();
		}
		var parsed = InputTokenizer.tokenize(content);
		var flags = parsed.getT1();
		var args = parsed.getT2();
		LOGGER.debug("content={}, prefixUsed={}, args={}, flags={}", content, prefixUsed, args, flags);
		if (args.isEmpty()) {
			LOGGER.debug("Prefix was used alone, without command name");
			return Optional.empty();
		}
		final String fPrefixUsed = prefixUsed;
		var command = commandMap.get(args.get(0));
		if (command == null) {
			LOGGER.debug("No command found");
		}
		return Optional.ofNullable(command)
				.map(cmd -> new ExecutableCommand(cmd, new Context(cmd, event, args, flags, bot, fPrefixUsed), errorHandler));
	}
	
	/**
	 * Gets the error handler assigned to this provider.
	 * 
	 * @return the error handler
	 */
	public CommandErrorHandler getErrorHandler() {
		return errorHandler;
	}
	
	
	
	@Override
	public String toString() {
		return "CommandProvider{commandMap=" + commandMap + ", errorHandler=" + errorHandler + "}";
	}
}
