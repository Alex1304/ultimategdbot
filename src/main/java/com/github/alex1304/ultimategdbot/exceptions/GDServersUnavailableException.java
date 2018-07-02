package com.github.alex1304.ultimategdbot.exceptions;

/**
 * Thrown when a command fails because GD servers are unavailable
 *
 * @author Alex1304
 */
public class GDServersUnavailableException extends CommandFailedException {

	private static final long serialVersionUID = -4335847212651533825L;

	public GDServersUnavailableException() {
		super("Geometry Dash servers are unavailable right now. Please try again later. ~~RobTop fix ur damn servers kthx~~");
	}

}
