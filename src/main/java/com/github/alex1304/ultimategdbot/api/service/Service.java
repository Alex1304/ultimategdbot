package com.github.alex1304.ultimategdbot.api.service;

import com.github.alex1304.ultimategdbot.api.Bot;

import reactor.core.publisher.Mono;

/**
 * Represents a service provided by the bot, accessible globally via
 * {@link ServiceContainer}.
 */
public interface Service {
	
	/**
	 * Gets the name of the service.
	 * 
	 * @return the name
	 */
	String getName();
	
	/**
	 * Performs an action on this service when the bot is ready.
	 * 
	 * @param bot the bot instance
	 * @return a Mono completing when the ready action is complete
	 */
	Mono<Void> onReady(Bot bot);
}
