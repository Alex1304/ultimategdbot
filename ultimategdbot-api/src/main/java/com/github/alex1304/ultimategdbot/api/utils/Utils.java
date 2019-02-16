package com.github.alex1304.ultimategdbot.api.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Command;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contains various utility methods.
 */
public class Utils {
	private Utils() {
	}
	
	/**
	 * Generates a default documentation for the command in a String format.
	 * The context is used to adapt the doc to the context (prefix, subcommands used, etc).
	 * 
	 * @param cmd - the command to generate the doc for
	 * @param ctx - the context to take into account in the generation of the doc
	 * @return the documentation
	 */
	public static String generateDefaultDocumentation(Command cmd, String prefix, String cmdName) {
		Objects.requireNonNull(cmd);
		Objects.requireNonNull(prefix);
		Objects.requireNonNull(cmdName);
		var sb = new StringBuilder();
		sb.append("```diff\n");
		sb.append(prefix);
		sb.append(cmdName);
		sb.append(' ');
		sb.append(cmd.getSyntax());
		sb.append("\n```\n");
		sb.append(cmd.getDescription());
		if (!cmd.getSubcommands().isEmpty()) {
			sb.append("\n\n**Subcommands:**\n```\n");
			cmd.getSubcommands().forEach(scmd -> {
				sb.append(prefix);
				sb.append(cmdName);
				sb.append(' ');
				sb.append(joinAliases(scmd.getAliases()));
				sb.append(' ');
				sb.append(scmd.getSyntax());
				sb.append("\n\t-> ");
				sb.append(scmd.getDescription());
				sb.append("\n");
			});
			sb.append("\n```\n");
		}
		return sb.toString();
	}
	
	public static String joinAliases(Set<String> aliases) {
		if (aliases.size() == 1) {
			return aliases.stream().findAny().get();
		} else {
			var aliasJoiner = new StringJoiner("|");
			aliases.stream().sorted((a, b) -> b.length() == a.length() ? a.compareTo(b) : b.length() - a.length())
					.forEach(aliasJoiner::add);
			return aliasJoiner.toString();
		}
	}
	
	public static int occurrences(String str, String substr) {
		int res = 0;
		for (var i = 0 ; i < str.length() - substr.length() + 1 ; i++) {
			var substr0 = str.substring(i, i + substr.length());
			if (substr.equals(substr0)) {
				res++;
			}
		}
		return res;
	}
	
	public static List<String> chunkMessage(String superLongMessage) {
		var chunks = new ArrayList<String>();
		var charactersRead = 0;
		final var breakpoint = 1990;
		var currentChunk = new StringBuilder();
		var inCodeblock = false;
		for (var line : superLongMessage.lines().collect(Collectors.toList())) {
			inCodeblock = occurrences(line, "```") % 2 == 1 ? !inCodeblock : inCodeblock;
			var old = charactersRead;
			charactersRead += line.length();
			if (old / breakpoint != charactersRead / breakpoint) {
				if (inCodeblock) {
					currentChunk.append("```\n");
				}
				chunks.add(currentChunk.substring(0, Math.min(currentChunk.length(), breakpoint)).toString());
				currentChunk.delete(0, currentChunk.length());
			} else {
				if (!chunks.isEmpty() && currentChunk.length() == 0) {
					if (inCodeblock) {
						currentChunk.append("```\n");
					}
				}
				currentChunk.append(line);
				currentChunk.append('\n');
			}
		}
		if (currentChunk.length() != 0) {
			chunks.add(currentChunk.toString());
		}
		return chunks;
	}
	
	public static Flux<Message> sendMultipleMessagesToOneChannel(Mono<Channel> channel, Iterable<Consumer<MessageCreateSpec>> specs) {
		return channel.ofType(MessageChannel.class).flatMapMany(c -> Flux.fromIterable(specs).flatMap(spec -> c.createMessage(spec)));
	}
	
	public static Flux<Message> sendMultipleSimpleMessagesToOneChannel(Mono<Channel> channel, Iterable<String> strings) {
		return channel.ofType(MessageChannel.class).flatMapMany(c -> Flux.fromIterable(strings).flatMap(spec -> c.createMessage(spec)));
	}
	
	public static Flux<Message> sendOneMessageToMultipleChannels(Flux<Channel> channels, Consumer<MessageCreateSpec> spec) {
		return channels.ofType(MessageChannel.class).flatMap(c -> c.createMessage(spec));
	}
	
	public static List<String> parseArgs(String input, String prefix) {
		if (input.startsWith(prefix)) {
			var inputWithoutPrefix = input.substring(prefix.length());
			return parseArgs("\"" + prefix + "\"" + inputWithoutPrefix);
		}
		return parseArgs(input);
	}
	
	public static List<String> parseArgs(String input) {
		var args = new ArrayList<String>();
		var buffer = new StringBuilder();
		var inQuotes = false;
		for (var arg : input.split("[ \n\t]")) {
			buffer.append((buffer.length() > 0 ? " " : "") + arg);
			if (occurrences(arg, "\"") % 2 == 1) {
				inQuotes = !inQuotes;
			}
			var isSpaceEscaped = false;
			if (!inQuotes && arg.endsWith("\\")) {
				buffer.deleteCharAt(buffer.length() - 1);
				isSpaceEscaped = true;
			}
			if (!inQuotes && !isSpaceEscaped) {
				args.add(buffer.toString().replaceAll("\"", ""));
				buffer.delete(0, buffer.length());
			}
		}
		args.add(buffer.toString().replaceAll("\"", ""));
		args.removeIf(String::isEmpty);
		return args;
	}
}
