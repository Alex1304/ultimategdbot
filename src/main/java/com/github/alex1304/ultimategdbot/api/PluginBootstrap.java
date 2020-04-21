package com.github.alex1304.ultimategdbot.api;

import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import reactor.core.publisher.Mono;

/**
 * Configures and creates a plugin.
 */
public interface PluginBootstrap {

	/**
	 * Creates a {@link Plugin} derived from the given bot and properties.
	 * 
	 * @param bot the bot instance
	 * @param pluginProperties the plugin properties
	 * @return the configured {@link Plugin}
	 */
	Mono<Plugin> setup(Bot bot, PropertyReader pluginProperties);
	
	/**
	 * Initializes the plugin properties.
	 * 
	 * @return the plugin properties
	 */
	Mono<PropertyReader> initPluginProperties();
}
