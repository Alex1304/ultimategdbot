package com.github.alex1304.ultimategdbot.api.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class CommandProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandProvider.class);

	private CommandErrorHandler errorHandler = new CommandErrorHandler();
	private final Map<String, Command> commandMap = new HashMap<>();
	
	public void setErrorHandler(CommandErrorHandler errorHandler) {
		this.errorHandler = Objects.requireNonNull(errorHandler);
	}
	
	public void add(Command command) {
		for (var alias : command.getAliases()) {
			commandMap.put(alias, command);
		}
	}
	
	public void addAnnotated(Object annotatedCommand) {
		add(AnnotatedCommand.buildFromAnnotatedObject(annotatedCommand));
	}
	
	public Optional<ExecutableCommand> provideFromEvent(Bot bot, String guildSpecificPrefix, MessageCreateEvent event) {
		var botId = bot.getMainDiscordClient().getSelfId();
		var prefixes = Set.of("<@" + botId + ">", "<@!" + botId + ">", guildSpecificPrefix);
		var content = event.getMessage().getContent().orElse("");
		String prefixUsed = null;
		for (var prefix : prefixes) {
			if (content.toLowerCase().startsWith(prefix.toLowerCase())) {
				content = content.substring(prefix.length());
				prefixUsed = prefix;
				break;
			}
		}
		if (prefixUsed == null) {
			LOGGER.debug("Message doesn't match any prefix");
			return Optional.empty();
		}
		var parsed = processTokens(tokenize(content));
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
	
	public CommandErrorHandler getErrorHandler() {
		return errorHandler;
	}
	
	private static List<String> tokenize(String input) {
		var tokens = new ArrayList<String>();
		var buffer = new StringBuilder();
		var inQuotes = false;
		var escaping = false;
		for (var c : input.strip().toCharArray()) {
			if (!escaping) {
				if (c == '\\') {
					escaping = true;
					continue;
				} else if (c == '"') {
					inQuotes = !inQuotes;
					continue;
				}
			}
			if (!inQuotes) {
				if (Character.isWhitespace(c)) {
					if (buffer.length() > 0) {
						tokens.add(buffer.toString());
						buffer.delete(0, buffer.length());
					}
				} else {
					buffer.append(c);
				}
			} else {
				buffer.append(c);
			}
			escaping = false;
		}
		if (buffer.length() != 0) {
			tokens.add(buffer.toString());
		}
		return tokens;
	}
	
	private static Tuple2<Flags, List<String>> processTokens(List<String> tokens) {
		var flags = Flags.builder();
		var args = new ArrayList<String>();
		if (!tokens.isEmpty()) {
			var tokenDeque = new ArrayDeque<>(tokens);
			while (!tokenDeque.isEmpty()) {
				var token = tokenDeque.remove();
				if (token.startsWith("--") && token.length() > 2) {
					var split = token.substring(2).split("=", 2);
					if (split.length == 1) {
						flags.add(split[0]);
					} else {
						flags.add(split[0], split[1]);
					}
				} else {
					args.add(token);
				}
			}
		}
		return Tuples.of(flags.build(), args);
	}
	
	@Override
	public String toString() {
		return "CommandProvider{commandMap=" + commandMap + "}";
	}
}
