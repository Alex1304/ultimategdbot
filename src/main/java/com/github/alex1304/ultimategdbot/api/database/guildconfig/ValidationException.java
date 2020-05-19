package com.github.alex1304.ultimategdbot.api.database.guildconfig;

/**
 * Thrown when the validation of a value fails.
 */
public class ValidationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ValidationException(String message) {
		super(message);
	}
}