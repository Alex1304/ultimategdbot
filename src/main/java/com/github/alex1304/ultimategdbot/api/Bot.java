package com.github.alex1304.ultimategdbot.api;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.service.Service;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself.
 */
public interface Bot extends Translator {
	
	/**
	 * Gets the config of the bot for the given name.
	 * 
	 * @return the config
	 */
	PropertyReader config(String name);
	
	/**
	 * Retrieves a service from the given class. The service must have been added on
	 * setup by one of the plugins.
	 * 
	 * @param <S>         the type of service
	 * @param serviceType the class of the service
	 * @return the service instance
	 * @throws IllegalArgumentException if the class does not correspond to a
	 *                                  registered service
	 */
	<S extends Service> S service(Class<S> serviceType);
	
	/**
	 * Tells whether a service of the given type is loaded into the bot or not.
	 * 
	 * @param serviceType the type of service to check
	 * @return true if a service of the given type is loaded, false otherwise
	 */
	boolean hasService(Class<? extends Service> serviceType);

	/**
	 * Gets the REST client of the bot.
	 * 
	 * @return the Discord client
	 */
	DiscordClient rest();

	/**
	 * Gets the gateway client of the bot.
	 * 
	 * @return the gateway client
	 */
	GatewayDiscordClient gateway();

	/**
	 * Gets a Set containing all successfully loaded plugins.
	 * 
	 * @return a Set of Plugin
	 */
	Set<PluginMetadata> plugins();
	
	/**
	 * Gets the bot owner.
	 * 
	 * @return a Mono emitting the bot owner
	 */
	Mono<User> owner();

	/**
	 * Logs a message.
	 * 
	 * @param message the message to log
	 * @return a Mono completing when logging is successful
	 */
	Mono<Void> log(String message);
	
	/**
	 * Starts the bot. This method blocks until the bot disconnects.
	 * 
	 * @return a Mono that gives asynchronous capabilities to the start method.
	 */
	Mono<Void> start();
}
