package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when a bot command fails
 *
 * @author Alex1304
 */
public class CommandFailedException extends Exception {

	private static final long serialVersionUID = -4409611658101363178L;

	public CommandFailedException(String arg0) {
		super(arg0);
	}
}
