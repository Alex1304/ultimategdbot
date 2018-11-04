package com.github.alex1304.ultimategdbot.utils;

import java.util.Arrays;
import java.util.List;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;

/**
 *
 *
 * @author Alex1304
 *
 */
public class Utils {

	private Utils() {
	}

	/**
	 * Creates a message spec using the given String as content
	 * 
	 * @param content - String
	 * @return MessageCreateSpec
	 */
	public static MessageCreateSpec messageOf(String content) {
		return new MessageCreateSpec().setContent(content);
	}

	/**
	 * Creates a message spec using the given EmbedCreateSpec as embed
	 * 
	 * @param embed - EmbedCreateSpec
	 * @return MessageCreateSpec
	 */
	public static MessageCreateSpec messageOf(EmbedCreateSpec embed) {
		return new MessageCreateSpec().setEmbed(embed);
	}

	/**
	 * Creates a message spec using the given String as content and the given
	 * EmbedCreateSpec as embed
	 * 
	 * @param content - String
	 * @param embed   - EmbedCreateSpec
	 * @return MessageCreateSpec
	 */
	public static MessageCreateSpec messageOf(String content, EmbedCreateSpec embed) {
		return new MessageCreateSpec().setContent(content).setEmbed(embed);
	}

	/**
	 * Extracts the arguments of a command and returns them in a list of strings.
	 * Note that the first argument is the name of the command, minus the prefix.
	 * For example, if the message is {@code !ping test test2} and the prefix is
	 * defined as {@code !}, then the returned list would be
	 * {@code ["ping", "test", "test2"]}
	 * 
	 * @param text - String
	 * @param prefix - String
	 * @return List&lt;String&gt;
	 */
	public static List<String> extractArgs(String text, String prefix) {
		var argsArray = text.split(" +");
		argsArray[0] = argsArray[0].substring(prefix.length());
		return Arrays.asList(argsArray);
	}
}
