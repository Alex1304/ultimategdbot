package com.github.alex1304.ultimategdbot.api.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class Markdown {
	
	private Markdown() {
	}
	
	/**
	 * Escapes characters used in Markdown syntax using a backslash
	 * 
	 * @param text the Markdown text to escape
	 * @return String
	 */
	public static String escape(String text) {
		List<Character> resultList = new ArrayList<>();
		Character[] charsToEscape = { '\\', '_', '*', '~', '`', ':', '@', '#', '|', '>' };
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
	 * Formats the text with bold markdown
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String bold(String text) {
		return "**" + text + "**";
	}

	/**
	 * Formats the text with italic markdown
	 * 
	 * @param text        the text to format
	 * @param starVariant whether to use stars (*) or underscores (_) to make italic
	 * @return the formatted text
	 */
	public static String italic(String text, boolean starVariant) {
		char italicChar = starVariant ? '*' : '_';
		return italicChar + text + italicChar;
	}
	
	/**
	 * Formats the text with italic markdown. Uses the star variant by default.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 * @see #italic(String, boolean)
	 */
	public static String italic(String text) {
		return italic(text, true);
	}

	/**
	 * Formats the text with underline markdown.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String underline(String text) {
		return "__" + text + "__";
	}

	/**
	 * Formats the text with strikethrough markdown.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String strikethrough(String text) {
		return "~~" + text + "~~";
	}
	
	/**
	 * Formats the text with spoiler markdown.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String spoiler(String text) {
		return "||" + text + "||";
	}

	/**
	 * Make a masked link using markdown. Note that this only works in Discord embed
	 * fields and descriptions, not in regular messages.
	 * 
	 * @param clickableText the text that should be clickable
	 * @param url           the url to redirect to when the text is clicked
	 * @return the masked link markdown
	 */
	public static String maskedLink(String clickableText, String url) {
		return "[" + clickableText + "](" + url + ")";
	}
	
	/**
	 * Formats the text as code.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String code(String text) {
		return "`" + text + "`";
	}
	
	/**
	 * Formats the text as a code block.
	 * 
	 * @param text     the text to format
	 * @param language the language of the code (hint for syntax highlighting)
	 * @return the formatted text
	 */
	public static String codeBlock(String text, String language) {
		return "```" + language + "\n" + text + "\n```\n";
	}

	/**
	 * Formats the text as a code block. This overload does not specify a language
	 * parameter for syntax highlighting.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String codeBlock(String text) {
		return codeBlock(text, "");
	}
	
	/**
	 * Formats the text as a quote.
	 * 
	 * @param text the text to format
	 * @return the formatted text
	 */
	public static String quote(String text) {
		return text.lines().map(line -> "> " + line).collect(Collectors.joining("\n"));
	}
}
