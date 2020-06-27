package com.github.alex1304.ultimategdbot.api.service;

import com.github.alex1304.ultimategdbot.api.Bot;

import reactor.core.publisher.Mono;

/**
 * Factory that can create services.
 * 
 * @param <S> the type of service object it can create
 */
public interface ServiceFactory<S extends Service> {
	
	/**
	 * Asynchronously creates the service.
	 * 
	 * @param bot the bot instance
	 * @return a Mono emitting the service object
	 */
	Mono<S> create(Bot bot);
	
	/**
	 * Gets the type of the service.
	 * 
	 * @return a Class
	 */
	Class<S> serviceClass();
}
