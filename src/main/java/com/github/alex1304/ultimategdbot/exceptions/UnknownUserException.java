package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when the bot cannot fetch a user's profile while running a command
 *
 * @author Alex1304
 */
public class UnknownUserException extends CommandFailedException {

	private static final long serialVersionUID = -2257907590597604406L;

	public UnknownUserException() {
		super("Unknown user");
	}

}
