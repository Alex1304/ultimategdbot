package com.github.alex1304.ultimategdbot.api;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

/**
 * Contains configuration resources for the bot. A resource is a set of
 * properties that can be read via the {@link PropertyReader} interface.
 */
public final class BotConfig {
	
	private final Map<String, PropertyReader> resources;

	private BotConfig(Map<String, PropertyReader> resources) {
		this.resources = resources;
	}
	
	/**
	 * Creates a new {@link BotConfig} initialized with resources defined in the
	 * given map.
	 * 
	 * @param resources the map containing the configuration resources. The key is
	 *                  the name of the resource, the value is the resource itself
	 * @return a new {@link BotConfig} instance
	 */
	public static BotConfig fromResourceMap(Map<String, PropertyReader> resources) {
		requireNonNull(resources);
		return new BotConfig(resources);
	}
	
	/**
	 * Gets a configuration resource by the given name.
	 * 
	 * @param name the name of the resource
	 * @return the configuration resource associated with the name
	 * @throws IllegalArgumentException if the resource is not found
	 */
	public PropertyReader resource(String name) {
		var resource = resources.get(name);
		if (resource == null) {
			throw new IllegalArgumentException("The configuration resource '" + name + "' is undefined");
		}
		return resource;
	}
}
