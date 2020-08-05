package com.github.alex1304.ultimategdbot.api.metadata;

import java.util.Set;

import com.github.alex1304.ultimategdbot.api.PluginMetadata;

/**
 * Service that exposes the metadata of all plugins loaded in the bot.
 */
public final class PluginMetadataService {

	private final Set<PluginMetadata> pluginMetadataSet;

	public PluginMetadataService(Set<PluginMetadata> pluginMetadataSet) {
		this.pluginMetadataSet = pluginMetadataSet;
	}

	/**
	 * Gets an immutable set of metadata of all plugins loaded in the bot.
	 * 
	 * @return a set of plugin metadata
	 */
	public Set<PluginMetadata> all() {
		return pluginMetadataSet;
	}
}
