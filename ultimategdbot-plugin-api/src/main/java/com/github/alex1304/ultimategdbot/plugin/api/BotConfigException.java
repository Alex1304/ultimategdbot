package com.github.alex1304.ultimategdbot.plugin.api;

public class BotConfigException extends Exception {

	private static final long serialVersionUID = 5218226924221016745L;

	public BotConfigException() {
		super();
	}

	public BotConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BotConfigException(String message, Throwable cause) {
		super(message, cause);
	}

	public BotConfigException(String message) {
		super(message);
	}

	public BotConfigException(Throwable cause) {
		super(cause);
	}
}
