package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when an error occurs with the database during the execution of a command
 *
 * @author Alex1304
 */
public class DatabaseFailureException extends CommandFailedException {

	private static final long serialVersionUID = 8423790594546568750L;

	public DatabaseFailureException() {
		super("An error occured with the database and the command could not continue. Sorry for inconvenience, try again later");
	}

}
