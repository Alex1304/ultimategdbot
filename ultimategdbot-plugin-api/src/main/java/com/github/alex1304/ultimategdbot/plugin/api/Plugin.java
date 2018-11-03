package com.github.alex1304.ultimategdbot.plugin.api;

/**
 * A plugin is defined by a name
 * 
 * @author Alex1304
 *
 */
public interface Plugin {
	
	/**
	 * Gets the name of the plugin.
	 * 
	 * @return String
	 */
	String getName();
	
	/**
	 * Code executed when the plugin is installed.
	 * 
	 */
	void install() throws PluginInstallationException;
}
