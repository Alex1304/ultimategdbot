package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.PluginContainer;
import com.github.alex1304.ultimategdbot.plugin.api.Service;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;

import discord4j.core.event.domain.lifecycle.ReadyEvent;

public class ServicePluginLoader extends PluginLoader<Service> {

	ServicePluginLoader() {
		super(PluginLoader.DEFAULT_PLUGIN_DIR + "services/", Service.class);
	}

	@Override
	void bind(UltimateGDBot bot) {
		// Start all installed services when the bot is ready
		Objects.requireNonNull(bot).getDiscordClient().getEventDispatcher().on(ReadyEvent.class).subscribe(event -> {
			for (var s : PluginContainer.ofServices()) {
				s.start();
			}
		});
	}
}
