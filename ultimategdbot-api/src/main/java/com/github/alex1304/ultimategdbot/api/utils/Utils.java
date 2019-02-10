package com.github.alex1304.ultimategdbot.api.utils;

import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import com.github.alex1304.ultimategdbot.api.Command;

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
	
	private static String joinAliases(Set<String> aliases) {
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
		for (var i = 0 ; i < str.length() - substr.length() - 1 ; i++) {
			var substr0 = str.substring(i, i + substr.length());
			if (substr.equals(substr0)) {
				res++;
			}
		}
		return res;
	}
}
