package com.github.alex1304.ultimategdbot.api.utils;

import java.util.Set;
import java.util.StringJoiner;

import com.github.alex1304.ultimategdbot.api.Command;

/**
 * Utilities for command documentation.
 */
public class CommandDocs {
	/**
	 * Generates the formatted documentation for the given command.
	 * 
	 * @param cmd - the command to generate docs for
	 * @return the doc
	 */
	public static String generate(Command cmd, String prefix) {
		var aliases = joinAliases(cmd.getAliases());
		var shortestAlias = shortestAlias(cmd.getAliases());
		var sb = new StringBuilder();
		sb.append("**Syntax:**\n```diff\n");
		sb.append(prefix);
		sb.append(joinAliases(cmd.getAliases()));
		sb.append('\n');
		sb.append(cmd.getSyntax());
		sb.append("\n```\n");
		sb.append(cmd.getDescription());
		sb.append("\n\n**Subcommands:**\n```\n");
		cmd.getSubcommands().forEach(scmd -> {
			sb.append(prefix);
			sb.append(shortestAlias);
			sb.append(' ');
			sb.append(joinAliases(scmd.getAliases()));
			sb.append(' ');
			sb.append(scmd.getSyntax());
			sb.append("\n\t-> ");
			sb.append(scmd.getDescription());
			sb.append("\n");
		});
		sb.append("\n```\n");
		
		return sb.toString();
	}
	
	private static String joinAliases(Set<String> aliases) {
		if (aliases.size() == 1) {
			return aliases.stream().findAny().get();
		} else {
			var aliasJoiner = new StringJoiner("|", "[", "]");
			aliases.forEach(aliasJoiner::add);
			return aliasJoiner.toString();
		}
	}
	
	private static String shortestAlias(Set<String> aliases) {
		return aliases.stream().sorted((a, b) -> a.length() == b.length() ? a.compareTo(b) : a.length() - b.length()).findFirst().get();
	}
}
