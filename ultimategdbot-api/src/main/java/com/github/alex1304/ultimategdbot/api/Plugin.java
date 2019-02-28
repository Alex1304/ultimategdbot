package com.github.alex1304.ultimategdbot.api;

import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;

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
	 * Gets a set of resource names that corresponds to database mapping files.
	 * Mapping files usually end with *.hbm.xml and should be located at the root of
	 * the {@code src/main/resources} directory of the plugin
	 * 
	 * @return a set containing the name of all mapping files used in the plugin.
	 */
	Set<String> getDatabaseMappingResources();
	
	/**
	 * Gets a map of configuration entries for guilds. Anything added here will be listed when using the setup command.
	 * 
	 * @param bot - the bot instance
	 * @return the guild configuration entries
	 */
	Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries(Bot bot);
	
	/**
	 * Code executed when the plugin is loaded. This allows the plugin to perform
	 * additional configuration.
	 * 
	 * @throws PluginSetupException exception that this method may throw to indicate
	 *                              that something went wrong when setting up the
	 *                              plugin. If this exception is thrown, the loading
	 *                              process of this plugin is cancelled.
	 */
	void setup() throws PluginSetupException;
}
