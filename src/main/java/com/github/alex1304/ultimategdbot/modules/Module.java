package com.github.alex1304.ultimategdbot.modules;

/**
 * A module is a functional component for the bot.
 * It can give the bot new capabilities when it comes to interact with other users.
 * They can be enabled and disabled during runtime..
 *
 * @author Alex1304
 */
public interface Module {
	
	/**
	 * Starts the module
	 */
	void start();
	
	/**
	 * Stops the module
	 */
	void stop();
	
	/**
	 * Restarts the module
	 */
	default void restart() {
		stop();
		start();
	}
}
