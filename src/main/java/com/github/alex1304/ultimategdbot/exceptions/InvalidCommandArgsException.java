package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when command args are invalid
 *
 * @author Alex1304
 */
public class InvalidCommandArgsException extends CommandFailedException {

	private static final long serialVersionUID = 1992625845197361512L;

	public InvalidCommandArgsException(String message) {
		super("Invalid command syntax. Please try one of the following:\n\n" + message);
	}

}