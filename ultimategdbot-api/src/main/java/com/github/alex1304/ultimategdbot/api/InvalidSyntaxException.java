package com.github.alex1304.ultimategdbot.api;

public class InvalidSyntaxException extends RuntimeException {
	private static final long serialVersionUID = -4784391332427416131L;
	
	private final Command cmd;

	public InvalidSyntaxException(Command cmd) {
		this.cmd = cmd;
	}

	/**
	 * Gets the command involved in the syntax error
	 * 
	 * @return the command
	 */
	public Command getCommand() {
		return cmd;
	}
}
