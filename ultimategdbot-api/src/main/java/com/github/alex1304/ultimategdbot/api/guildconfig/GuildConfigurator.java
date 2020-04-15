package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

public class GuildConfigurator<G extends GuildConfigData<G>> {
	
	static final VarHandle GUILD_CONFIG_DATA_REF;
	
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			GUILD_CONFIG_DATA_REF = lookup.findVarHandle(
					GuildConfigurator.class, "guildConfigData", GuildConfigData.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	private final Map<String, ConfigEntry<G, ?>> configEntries;

	private volatile G guildConfigData;
	
	private GuildConfigurator(G guildConfigData, Map<String, ConfigEntry<G, ?>> configEntries) {
		this.guildConfigData = guildConfigData;
		this.configEntries = configEntries;
	}
	
	/**
	 * Gets a collection of configuration entries containied in this 
	 * @return
	 */
	public Collection<ConfigEntry<G, ?>> getConfigEntries() {
		return Collections.unmodifiableCollection(configEntries.values());
	}
	
	/**
	 * Gets the configuration entry corresponding to the given key
	 * 
	 * @param key the key of the entry to get
	 * @return the entry
	 * @throws IllegalArgumentException if the key does not match any entry
	 */
	public ConfigEntry<G, ?> getConfigEntry(String key) {
		var entry = configEntries.get(key);
		if (entry == null) {
			throw new IllegalArgumentException("Configuration entry with key \"" + key + "\" not found");
		}
		return entry;
	}
	
	/**
	 * Gets the {@link GuildConfigData} instance containing all data that has been
	 * configured through this configurator.
	 * 
	 * @return 
	 */
	public G getData() {
		return guildConfigData;
	}
	
	void updateData(UnaryOperator<G> updateFunc) {
		for (;;) {
			var oldData = guildConfigData;
			var newData = updateFunc.apply(oldData);
			if (GUILD_CONFIG_DATA_REF.compareAndSet(this, oldData, newData)) {
				return;
			}
		}
	}

	public static <G extends GuildConfigData<G>> Builder<G> builder(G guildConfigData) {
		return new Builder<>(guildConfigData);
	}

	public static class Builder<G extends GuildConfigData<G>> {
		
		private final G guildConfigData;
		private final List<ConfigEntryBuilder<G, ?>> addedEntries = new ArrayList<>();
		
		private Builder(G guildConfigData) {
			this.guildConfigData = guildConfigData;
		}
		
		public <T> Builder<G> addEntry(ConfigEntryBuilder<G, ?> configEntryBuilder) {
			addedEntries.add(configEntryBuilder);
			return this;
		}
		
		public GuildConfigurator<G> build() {
			var configEntries = new TreeMap<String, ConfigEntry<G, ?>>();
			var configurator = new GuildConfigurator<>(guildConfigData, configEntries);
			addedEntries.forEach(added -> {
				var configEntry = added.build(configurator);
				configEntries.put(configEntry.getKey(), configEntry);
			});
			return configurator;
		}
	}
}
