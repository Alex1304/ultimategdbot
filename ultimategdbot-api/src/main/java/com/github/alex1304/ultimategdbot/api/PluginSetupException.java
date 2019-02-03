package com.github.alex1304.ultimategdbot.api;

public class PluginSetupException extends Exception {
	private static final long serialVersionUID = 4867037313712368209L;

	private PluginSetupException() {
		super();
	}

	private PluginSetupException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	private PluginSetupException(String message, Throwable cause) {
		super(message, cause);
	}

	private PluginSetupException(String message) {
		super(message);
	}

	private PluginSetupException(Throwable cause) {
		super(cause);
	}
	
}
