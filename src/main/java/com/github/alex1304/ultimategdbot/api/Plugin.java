package com.github.alex1304.ultimategdbot.api;

import com.github.alex1304.rdi.ServiceReference;

import reactor.core.publisher.Mono;

/**
 * Configures and creates a plugin.
 */
public interface Plugin {

	/**
	 * Defines the root service of the plugin.
	 * 
	 * @return the reference to the root service
	 */
	ServiceReference<?> rootService();
	
	/**
	 * Gets the metadata of this plugin. May be generated from an asynchronous
	 * source, but implementors are encouraged to cache the object for future calls.
	 * It is also encouraged to consistently return the same data when called
	 * several times in the same running JVM.
	 * 
	 * @return a Mono emitting the metadata of this plugin.
	 */
	Mono<PluginMetadata> metadata();
}
