package com.github.alex1304.ultimategdbot.exceptions;

import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

/**
 * Thrown when a command fails because GD servers are unavailable
 *
 * @author Alex1304
 */
public class GDServersUnavailableException extends CommandFailedException {

	private static final long serialVersionUID = -4335847212651533825L;
	
	public GDServersUnavailableException() {
		super("Geometry Dash servers are unavailable right now. Please try again later.");
	}

	public GDServersUnavailableException(GDAPIException e) {
		this();
		UltimateGDBot.logException(e);
	}

}
