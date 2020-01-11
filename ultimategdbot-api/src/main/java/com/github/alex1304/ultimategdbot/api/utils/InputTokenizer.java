package com.github.alex1304.ultimategdbot.api.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.ultimategdbot.api.command.FlagSet;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Utility methods to split an input into tokens and recognize which are flags
 * and which are arguments.
 */
public class InputTokenizer {
	
	private InputTokenizer() {
	}
	
	/**
	 * Splits the input into tokens. A token is delimited by double quotes or by
	 * whitespaces if there are none. A whitespace character can be escaped using a
	 * backslash to indicate that it should not delimit a token, and a double quote
	 * can be escaped the same way to indicate that it is intended to be a litteral
	 * character and not a token delimiter. Once tokens are extracted, they are
	 * separated into flags and arguments. A token starting with "--" followed by at
	 * least 1 character is treated as a flag, the rest are arguments.
	 * 
	 * @param flagPrefix the prefix used to introduce command flags
	 * @param input the input to tokenize
	 * @return a Tuple2 which first value is a {@link FlagSet} that contains the
	 *         tokens that are flags, and second value is a List containing the
	 *         tokens that are arguments
	 */
	public static Tuple2<FlagSet, List<String>> tokenize(String flagPrefix, String input) {
		// Extracting the tokens
		var tokens = new ArrayDeque<String>();
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
		// Separating tokens into flags and args
		var flags = FlagSet.builder();
		var args = new ArrayList<String>();
		while (!tokens.isEmpty()) {
			var token = tokens.remove();
			if (token.startsWith(flagPrefix) && token.length() > flagPrefix.length()) {
				var split = token.substring(flagPrefix.length()).split("=", 2);
				if (split.length == 1) {
					flags.add(split[0]);
				} else {
					flags.add(split[0], split[1]);
				}
			} else {
				args.add(token);
			}
		}
		return Tuples.of(flags.build(), args);
	}
}
