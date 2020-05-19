package com.github.alex1304.ultimategdbot.api.command.menu;

public class UnexpectedReplyException extends RuntimeException {
	
	private static final long serialVersionUID = 8882968955193503165L;

	public UnexpectedReplyException(String message) {
		super(message);
	}

}
