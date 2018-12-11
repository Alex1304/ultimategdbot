package com.github.alex1304.ultimategdbot.utils;

import java.util.Arrays;
import java.util.List;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Utility methods for the bot
 *
 * @author Alex1304
 *
 */
public class Utils {

	private Utils() {
	}

	/**
	 * Extracts the arguments of a command and returns them in a list of strings.
	 * Note that the first argument is the name of the command, minus the prefix.
	 * For example, if the message is {@code !ping test test2} and the prefix is
	 * defined as {@code !}, then the returned list would be
	 * {@code ["ping", "test", "test2"]}
	 * 
	 * @param text   - String
	 * @param prefix - String
	 * @return List&lt;String&gt;
	 */
	public static List<String> extractArgs(String text, String prefix) {
		var argsArray = text.split(" +");
		argsArray[0] = argsArray[0].substring(prefix.length());
		return Arrays.asList(argsArray);
	}

	/**
	 * Sends a message as a reply to a previous message. It means that it will send
	 * a message in the same channel as the previous message.
	 * 
	 * @param event   - The event emitted after receiving the original message
	 * @param message - The content as String of the message to send back
	 * @return Mono&lt;MessageChannel&gt;
	 */
	public static Mono<Message> reply(MessageCreateEvent event, String message) {
		return event.getMessage().getChannel().flatMap(c -> c.createMessage(message));
	}

	/**
	 * Sends a message as a reply to a previous message. It means that it will send
	 * a message in the same channel as the previous message.
	 * 
	 * @param event   - The event emitted after receiving the original message
	 * @param message - The content as MessageCreateSpec of the message to send back
	 * @return Mono&lt;MessageChannel&gt;
	 */
	public static Mono<Message> reply(MessageCreateEvent event, MessageCreateSpec message) {
		return event.getMessage().getChannel().flatMap(c -> c.createMessage(message));
	}

	/**
	 * Reads the value of an option in the arguments. An option is a special
	 * argument starting with a dash for short options, and with double dash for
	 * long options, basically like Unix shell commands.
	 * 
	 * @param optShort - the name of the short option to look for, WITHOUT the dash
	 * @param optLong  - the name of the long option to look for, WITHOUT the double
	 *                 dash
	 * @param args     - the arguments
	 * @param maxArgs  - stop reading the option value after {@code maxArgs}
	 *                 arguments/words. This value may be negative, if so there
	 *                 won't be any limit.
	 * @return the String representig the read option value. Returns null if the
	 *         option was not found. An empty string means that the option is
	 *         present but has no value.
	 */
	public static String getArgOption(String optShort, String optLong, List<String> args, int maxArgs) {
		final var optShortPrefix = "-";
		final var optLongPrefix = "--";
		final var isInfinite = maxArgs < 0;

		StringBuilder sb = new StringBuilder();
		var nbArgs = 0;
		var space = "";
		var capturing = false;
		for (var arg : args) {
			if (capturing) {
				if (arg.startsWith(optShortPrefix) || (!isInfinite && nbArgs >= maxArgs)) {
					return sb.toString();
				}
				sb.append(space).append(arg);
				space = " ";
				nbArgs++;
			} else {
				capturing = arg.equals(optShortPrefix + optShort) || arg.equals(optLongPrefix + optLong);
			}
		}

		return capturing ? sb.toString() : null;
	}
}
