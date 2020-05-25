package com.github.alex1304.ultimategdbot.api;

import com.github.alex1304.ultimategdbot.api.service.ServiceDependant;

import reactor.core.publisher.Mono;

/**
 * Configures and creates a plugin.
 */
public interface Plugin extends ServiceDependant {

	/**
	 * Configures a {@link Plugin} for the given bot.
	 * 
	 * @param bot the bot instance
	 * @return a Mono completing when setup is complete
	 */
	Mono<Void> setup(Bot bot);
	
	/**
	 * Gets the metadata of this plugin. May be generated from an asynchronous
	 * source, but implementors are encourage to cache the object for future calls.
	 * It is also encouraged to consistently return the same data when called
	 * several times in the same running JVM.
	 * 
	 * @return a Mono emitting the metadata of this plugin.
	 */
	Mono<PluginMetadata> metadata();
}
