package com.github.alex1304.ultimategdbot.utils;

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

}
