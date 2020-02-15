package com.github.alex1304.ultimategdbot.api;

import reactor.core.publisher.Mono;

public interface PluginBootstrap {

	Mono<Plugin> setup(Bot bot);
}
