package com.github.alex1304.ultimategdbot.api;

import java.util.Set;

/**
 * Represents a plugin. A plugin has a name and provides a list of commands.
 */
public interface Plugin {
	/**
	 * Gets the set of commands that this plugin provides.
	 * 
	 * @return a set of commands
	 */
	Set<Command> getProvidedCommands();
	
	/**
	 * Gets the name of the plugin.
	 * 
	 * @return the name
	 */
	String getName();
	
	/**
	 * Called once when the plugin is loaded on startup. You can make verifications
	 * here to ensure that your plugin has everything needed to run properly. If it
	 * isn't the case, you can throw a {@link PluginSetupException} to cancel the
	 * loading of this plugin and display a warning message on bot startup.
	 * 
	 * @throws PluginSetupException if the plugin is not in a state that allows it
	 *                              to be properly loaded.
	 */
	void setup() throws PluginSetupException;
}
