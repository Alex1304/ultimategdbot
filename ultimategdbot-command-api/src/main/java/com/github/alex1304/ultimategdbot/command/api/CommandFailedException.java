package com.github.alex1304.ultimategdbot.command.api;

public class CommandFailedException extends Exception {

	private static final long serialVersionUID = 2617706054919523892L;

	public CommandFailedException(String message) {
		super(message);
	}

}
