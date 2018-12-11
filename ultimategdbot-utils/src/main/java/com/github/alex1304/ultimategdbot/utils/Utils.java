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
}
