package com.github.alex1304.ultimategdbot.plugin.api;

public class CommandInitializationException extends Exception {

	private static final long serialVersionUID = 5218226924221016745L;

	public CommandInitializationException() {
		super();
	}

	public CommandInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CommandInitializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandInitializationException(String message) {
		super(message);
	}

	public CommandInitializationException(Throwable cause) {
		super(cause);
	}
}
