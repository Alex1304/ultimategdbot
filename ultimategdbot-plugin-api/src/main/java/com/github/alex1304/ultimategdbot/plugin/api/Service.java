package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.Map;

/**
 * A service is a task that is run by the bot, launched at startup and that
 * doesn't need a command to be triggered.
 * 
 * @author Alex1304
 *
 */
public interface Service extends Plugin {

	/**
	 * Starts the service. If the service is already started, calling this method
	 * should do nothing.
	 */
	void start();

	/**
	 * Stops the service. If the service is already started, calling this method
	 * should do nothing. This method should cancel everything done by a previous
	 * call of {@link Service#start()}. It should be designed so that a succession
	 * of start and stop calls should produce a consistent behavior and be free of
	 * memory leaks.
	 */
	void stop();

	/**
	 * A service can define an interface of commands, which is simply a list of
	 * commands that can be used to get or modify the state of a service while it's
	 * running. Note that the commands to respectively start and stop the service
	 * are natively implemented.
	 * 
	 * @return Map&lt;String, Command&gt;
	 */
	Map<String, Command> commandInterface();

	/**
	 * Whether the service is started
	 * 
	 * @return boolean
	 */
	boolean isStarted();
}
