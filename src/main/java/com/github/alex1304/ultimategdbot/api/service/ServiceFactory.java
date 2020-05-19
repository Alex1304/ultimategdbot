package com.github.alex1304.ultimategdbot.api.service;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import reactor.core.publisher.Mono;

/**
 * Factory that can create services.
 * 
 * @param <S> the type of service object it can create
 */
public interface ServiceFactory<S extends Service> {
	
	/**
	 * Creates the service.
	 * 
	 * @param properties the bot's properties
	 * @return a Mono emitting the service object
	 */
	Mono<S> create(PropertyReader properties);
	
	/**
	 * Gets the type of the service.
	 * 
	 * @return a Class
	 */
	Class<S> type();
	
	/**
	 * Gets the name of the properties file where to read properties (without the
	 * .properties extension). Defaults to "config".
	 * 
	 * @return the properties file name
	 */
	default String propertiesFileName() {
		return "config";
	}
	
	/**
	 * Gets the other services required by this service.
	 * 
	 * @return a {@link Set} of {@link ServiceFactory}
	 */
	default Set<ServiceFactory<?>> requiredServices() {
		return Set.of();
	}
}
