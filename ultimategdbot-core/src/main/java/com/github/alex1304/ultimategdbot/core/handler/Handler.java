package com.github.alex1304.ultimategdbot.core.handler;

/**
 * Used by the bot to handle things coming from Discord (commands, menus, etc)
 */
public interface Handler {
	/**
	 * Allows the handler to perform preliminary actions before starting to listen
	 * to events.
	 */
	void prepare();
	
	/**
	 * Makes the handler listen to Discord events and starts operating.
	 */
	void listen();
}
