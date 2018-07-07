package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when a command is declared as unavailable
 *
 * @author Alex1304
 */
public class CommandUnavailableException extends CommandFailedException {

	private static final long serialVersionUID = 7917953728406571617L;

	public CommandUnavailableException() {
		super("This command is temporarily unavailable. Sorry for the inconvenience.");
	}

}
