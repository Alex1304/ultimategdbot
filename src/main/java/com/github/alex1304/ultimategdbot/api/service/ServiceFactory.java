package com.github.alex1304.ultimategdbot.api.service;

import com.github.alex1304.ultimategdbot.api.Bot;

/**
 * Factory that can create services.
 * 
 * @param <S> the type of service object it can create
 */
public interface ServiceFactory<S extends Service> extends ServiceDependant {
	
	/**
	 * Creates the service.
	 * 
	 * @param properties the bot's properties
	 * @return a Mono emitting the service object
	 */
	S create(Bot bot);
	
	/**
	 * Gets the type of the service.
	 * 
	 * @return a Class
	 */
	Class<S> serviceClass();
}
