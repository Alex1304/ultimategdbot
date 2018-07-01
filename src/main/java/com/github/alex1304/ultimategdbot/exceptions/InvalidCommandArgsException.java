package com.github.alex1304.ultimategdbot.exceptions;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

/**
 * Thrown when command args are invalid
 *
 * @author Alex1304
 */
public class InvalidCommandArgsException extends CommandFailedException {

	private static final long serialVersionUID = 1992625845197361512L;

	public InvalidCommandArgsException() {
		super("Invalid command syntax. Try " + UltimateGDBot.property("ultimategdbot.prefix.canonical")
				+ "help if you have any trouble.");
	}

}