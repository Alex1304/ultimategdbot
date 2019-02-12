package com.github.alex1304.ultimategdbot.core.handler;

/**
 * Used by the bot to handle things coming from Discord (commands, menus, etc)
 */
public interface Handler {
	/**
	 * Makes the handler listen to Discord events and starts operating.
	 */
	void listen();
}
