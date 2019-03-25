package com.github.alex1304.ultimategdbot.api.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contains various utility methods.
 */
public class BotUtils {
	private BotUtils() {
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
		sb.append("\n");
		sb.append(cmd.getLongDescription());
		sb.append("\n");
		if (!cmd.getSubcommands().isEmpty()) {
			sb.append("\n**Subcommands:**\n```\n");
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
	
	public static List<String> chunkMessage(String superLongMessage, int maxCharacters) {
		var chunks = new ArrayList<String>();
		var currentChunk = new StringBuilder();
		var inCodeblock = false;
		for (var line : superLongMessage.lines().collect(Collectors.toList())) {
			inCodeblock = occurrences(line, "```") % 2 == 1 ? !inCodeblock : inCodeblock;
			if (currentChunk.length() + line.length() + 1 >= maxCharacters) {
				if (inCodeblock) {
					currentChunk.append("```\n");
				}
				chunks.add(currentChunk.substring(0, Math.min(currentChunk.length(), maxCharacters)).toString());
				currentChunk.delete(0, currentChunk.length());
			} else {
				if (!chunks.isEmpty() && currentChunk.length() == 0) {
					if (inCodeblock) {
						currentChunk.append("```\n");
					}
				}
			}
			currentChunk.append(line);
			currentChunk.append('\n');
		}
		chunks.add(currentChunk.toString());
		return chunks;
	}
	
	public static List<String> chunkMessage(String superLongMessage) {
		return chunkMessage(superLongMessage, Message.MAX_CONTENT_LENGTH - 10);
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
		if (prefix.equalsIgnoreCase(input.substring(0, Math.min(input.length(), prefix.length())))) {
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
			if (arg.isEmpty()) {
				continue;
			}
			buffer.append((buffer.length() > 0 ? " " : "") + arg);
			if ((occurrences(arg, "\"") - occurrences(arg, "\\\"")) % 2 == 1) {
				inQuotes = !inQuotes;
			}
			var isSpaceEscaped = false;
			if (!inQuotes && arg.endsWith("\\")) {
				buffer.deleteCharAt(buffer.length() - 1);
				isSpaceEscaped = true;
			}
			if (!inQuotes && !isSpaceEscaped) {
				args.add(removeQuotesUnlessEscaped(buffer.toString()));
				buffer.delete(0, buffer.length());
			}
		}
		if (buffer.length() != 0) {
			args.add(removeQuotesUnlessEscaped(buffer.toString()));
		}
		return args;
	}
	
	public static String removeQuotesUnlessEscaped(String text) {
		var sb = new StringBuilder();
		char prev = 0;
		for (var c : text.toCharArray()) {
			if (prev != '\\' && c == '"') continue;
			sb.append(c);
			prev = c;
		}
		return sb.toString();
	}
	
	/**
	 * Escapes characters used in Markdown syntax using a backslash
	 * 
	 * @param text the Markdown text to escape
	 * @return String
	 */
	public static String escapeMarkdown(String text) {
		List<Character> resultList = new ArrayList<>();
		Character[] charsToEscape = { '\\', '_', '*', '~', '`', ':', '@', '#', '|' };
		List<Character> charsToEscapeList = Arrays.asList(charsToEscape);
		
		for (char c : text.toCharArray()) {
			if (charsToEscapeList.contains(c))
				resultList.add('\\');
			resultList.add(c);
		}
		
		char[] result = new char[resultList.size()];
		for (int i = 0 ; i < result.length ; i++)
			result[i] = resultList.get(i);
		
		return new String(result);
	}
	
	/**
	 * Formats the username of the user specified as argument with the format username#discriminator
	 * @param user - The user whom username will be formatted
	 * @return The formatted username as String.
	 */
	public static String formatDiscordUsername(User user) {
		return escapeMarkdown(user.getUsername() + "#" + user.getDiscriminator());
	}
	
	public static Mono<User> convertStringToUser(Bot bot, String str) {
		String id;
		if (str.matches("[0-9]{1,19}")) {
			id = str;
		} else if (str.matches("<@!?[0-9]{1,19}>")) {
			id = str.substring(str.startsWith("<@!") ? 3 : 2, str.length() - 1);
		} else {
			return Mono.error(new CommandFailedException("Not a valid mention/ID."));
		}
		return Mono.just(id)
				.map(Snowflake::of)
				.onErrorMap(e -> new CommandFailedException("Not a valid mention/ID."))
				.flatMap(snowflake -> bot.getDiscordClients().flatMap(client -> client.getUserById(snowflake)).next())
				.onErrorMap(e -> new CommandFailedException("Could not resolve the mention/ID to a valid user."));
	}
	
	public static String formatTimeMillis(Duration time) {
		var result = (time.toDaysPart() > 0 ? time.toDaysPart() + "d " : "")
				+ (time.toHoursPart() > 0 ? time.toHoursPart() + "h " : "")
				+ (time.toMinutesPart() > 0 ? time.toMinutesPart() + "min " : "")
				+ (time.toSecondsPart() > 0 ? time.toSecondsPart() + "s " : "")
				+ (time.toMillisPart() > 0 ? time.toMillisPart() + "ms " : "");
		return result.isEmpty() ? "0ms" : result.substring(0, result.length() - 1);
	}
}
