package com.github.alex1304.ultimategdbot.plugin.api;

public class CommandFailedException extends RuntimeException {

	private static final long serialVersionUID = -7535649738459999884L;

	public CommandFailedException() {
		super();
	}

	public CommandFailedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public CommandFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommandFailedException(String message) {
		super(message);
	}

	public CommandFailedException(Throwable cause) {
		super(cause);
	}
}
