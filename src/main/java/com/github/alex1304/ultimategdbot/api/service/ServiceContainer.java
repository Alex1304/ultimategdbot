package com.github.alex1304.ultimategdbot.api.service;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;

import com.github.alex1304.ultimategdbot.api.Bot;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Load, store and access services. Services are expected to be registered via the add method.
 * In order to use the services, they first need to be loaded 
 */
public class ServiceContainer {
	
	private static final VarHandle LOADING_STATE_REF;
	
	static {
		try {
			var lookup = MethodHandles.lookup();
			LOADING_STATE_REF = lookup.findVarHandle(ServiceContainer.class, "loadingState", LoadingState.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	private final ConcurrentHashMap<Class<? extends Service>, Mono<? extends Service>> declaredServices = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Class<? extends Service>, Service> servicesReady = new ConcurrentHashMap<>();
	private final Bot bot;
	private volatile LoadingState loadingState = LoadingState.NOT_LOADED;
	
	public ServiceContainer(Bot bot) {
		this.bot = bot;
	}

	/**
	 * Adds a new service to this container. All services must be added before
	 * calling {@link #load()}.
	 * 
	 * @param factory the service factory that instantates the service on load
	 * @throws IllegalStateException if {@link #load()} has already been executed
	 */
	public void add(ServiceFactory<?> factory) {
		if (loadingState != LoadingState.NOT_LOADED) {
			throw new IllegalStateException("Cannot add more services after load");
		}
		declaredServices.putIfAbsent(factory.type(), factory.create(bot.properties(factory.propertiesFileName())));
	}
	
	/**
	 * Loads all services that have been added to this container. The factories will
	 * be subscribed to and the resulting {@link Service} instances will be cached.
	 * Completion of the returned Mono means all services are loaded and ready to
	 * use. As soon as services start loading, it won't be possible to add more
	 * services via {@link #add(ServiceFactory)} anymore.
	 * 
	 * <p>
	 * This method may only be subscribed to once. Attempting to load the container
	 * several times will result in an {@link IllegalStateException}.
	 * 
	 * @return a Mono which completion indicates that all services are loaded, or an
	 *         error if something goes wrong during loading process.
	 */
	public Mono<Void> load() {
		return Mono.fromRunnable(() -> {
					if (!LOADING_STATE_REF.compareAndSet(this, LoadingState.NOT_LOADED, LoadingState.LOADING)) {
						throw new IllegalStateException("Attempted to load the service container more than once");
					}
				})
				.thenMany(Flux.fromIterable(declaredServices.entrySet()))
				.flatMap(entry -> entry.getValue().map(service -> servicesReady.put(entry.getKey(), service)))
				.doOnError(e -> loadingState = LoadingState.NOT_LOADED)
				.then(Mono.fromRunnable(() -> loadingState = LoadingState.LOADED));
	}
	
	/**
	 * Gets a service that has been loaded in this container.
	 * 
	 * @param <S>         the type of service
	 * @param serviceType the class of the service
	 * @return the service instance
	 * @throws IllegalArgumentException if no service for the given class is found
	 * @throws IllegalStateException    if {@link #load()} has never been called
	 *                                  beforehand
	 */
	@SuppressWarnings("unchecked")
	public <S extends Service> S get(Class<S> serviceType) {
		if (loadingState != LoadingState.LOADED) {
			throw new IllegalStateException("Services are not loaded");
		}
		var service = servicesReady.get(serviceType);
		if (service == null) {
			throw new IllegalArgumentException("No service registered for class " + serviceType.getName());
		}
		return (S) service;
	}

	public enum LoadingState {
		NOT_LOADED, LOADING, LOADED;
	}
}
