package com.github.alex1304.ultimategdbot.api;

import reactor.core.publisher.Mono;

/**
 * Configures and creates a plugin.
 */
public interface PluginBootstrap {

	/**
	 * Configures a {@link Plugin} for the given bot.
	 * 
	 * @param bot the bot instance
	 * @return the configured {@link Plugin}
	 */
	Mono<Plugin> setup(Bot bot);
}
