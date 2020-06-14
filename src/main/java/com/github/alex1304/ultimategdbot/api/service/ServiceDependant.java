package com.github.alex1304.ultimategdbot.api.service;

import java.util.Set;

/**
 * A class implementing this interface may depend on other services to run.
 */
public interface ServiceDependant {

	/**
	 * Gets the services required by the plugin.
	 * 
	 * @return a {@link Set} of {@link ServiceFactory}
	 */
	default Set<Class<? extends Service>> requiredServices() {
		return Set.of();
	}
}
