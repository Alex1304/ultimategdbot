package com.github.alex1304.ultimategdbot.api.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the list of the arguments of a command.
 */
public class ArgumentList {

	private final List<String> tokens;
	
	public ArgumentList(List<String> tokens) {
		this.tokens = tokens;
	}

	/**
	 * Gets the number of tokens (arguments) present in the list.
	 * 
	 * @return the token count
	 */
	public int tokenCount() {
		return tokens.size();
	}
	
	/**
	 * Returns the argument at the specified position.
	 * 
	 * @param position the position of the argument
	 * @return the argument at the specified position
	 * @throws IndexOutOfBoundsException if the position is out of bounds
	 */
	public String get(int position) {
		return tokens.get(position);
	}
	
	/**
	 * Gets all arguments from the specified position to the last one, into one
	 * String resulting of the concatenation of all arguments (separated with a
	 * whitespace). Specifying a position <= 0 returns all arguments. Specifying a
	 * position >= tokenCount will result in an empty String.
	 * 
	 * @param position the position of the first arguments
	 * @return all arguments into one concatenated String
	 */
	public String getAllAfter(int position) {
		return new ArrayDeque<>(getTokens(position + 1)).getLast();
	}

	public List<String> getTokens() {
		return Collections.unmodifiableList(tokens);
	}
	
	public List<String> getTokens(int maxCount) {
		var mergedTokens = new ArrayDeque<>(tokens);
		while (mergedTokens.size() > 1 && mergedTokens.size() > maxCount) {
			var lastArg = mergedTokens.removeLast();
			var beforeLastArg = mergedTokens.removeLast();
			mergedTokens.addLast(beforeLastArg + " " + lastArg);
		}
		return Collections.unmodifiableList(new ArrayList<>(mergedTokens));
	}
}
