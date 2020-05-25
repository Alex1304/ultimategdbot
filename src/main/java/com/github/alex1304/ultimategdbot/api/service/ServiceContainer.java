package com.github.alex1304.ultimategdbot.api.service;

import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.api.Bot;

/**
 * Load, store and access services. Services are expected to be registered via the add method.
 * In order to use the services, they first need to be loaded 
 */
public class ServiceContainer {

	private final ConcurrentHashMap<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();
	private final Bot bot;
	
	public ServiceContainer(Bot bot) {
		this.bot = bot;
	}

	/**
	 * Adds a new service to this container. This method first checks if a service
	 * of the same type isn't already added. If it is the case, the factory is
	 * discarded and the service is not instantiated.
	 * 
	 * @param factory the factory that will instantate the service to add if not
	 *                already present
	 */
	public boolean add(ServiceFactory<?> factory) {
		var box = new Object() { private boolean computed = false; };
		services.computeIfAbsent(factory.serviceClass(), k -> {
			box.computed = true;
			return factory.create(bot);
		});
		return box.computed;
	}
	
	/**
	 * Gets a service that has been added in this container.
	 * 
	 * @param <S>         the type of service
	 * @param serviceType the class of the service
	 * @return the service instance
	 * @throws IllegalArgumentException if no service for the given class is found
	 */
	@SuppressWarnings("unchecked")
	public <S extends Service> S get(Class<S> serviceType) {
		var service = services.get(serviceType);
		if (service == null) {
			throw new IllegalArgumentException("No service registered for class " + serviceType.getName());
		}
		return (S) service;
	}
}
