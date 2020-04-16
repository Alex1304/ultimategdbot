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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * Allows to view and edit configuration for a guild. This high-level class is
 * backed by a lower-level database entity, and the configuration entries
 * represent an abstracted way to view or edit a specific field of that entity.
 * 
 * <p>
 * This class is thread-safe: getting the data after having modified it via the
 * different entries will always give the most up-to-date result, even if data
 * is being modified by concurrent threads.
 * </p>
 * 
 * @param <D> the type of database entity backing this configurator
 */
public class GuildConfigurator<D extends GuildConfigData<D>> {
	
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

	private final Map<String, ConfigEntry<?>> configEntries;

	private volatile D guildConfigData;
	
	private GuildConfigurator(D guildConfigData, Map<String, ConfigEntry<?>> configEntries) {
		this.guildConfigData = guildConfigData;
		this.configEntries = configEntries;
	}
	
	/**
	 * Gets a collection of configuration entries containied in this 
	 * @return
	 */
	public Collection<ConfigEntry<?>> getConfigEntries() {
		return Collections.unmodifiableCollection(configEntries.values());
	}
	
	/**
	 * Gets the configuration entry corresponding to the given key
	 * 
	 * @param key the key of the entry to get
	 * @return the entry
	 * @throws IllegalArgumentException if the key does not match any entry
	 */
	public ConfigEntry<?> getConfigEntry(String key) {
		var entry = configEntries.get(key);
		if (entry == null) {
			throw new IllegalArgumentException("Configuration entry with key \"" + key + "\" not found");
		}
		return entry;
	}
	
	/**
	 * Gets the {@link GuildConfigData} instance reflecting all modifications
	 * performed through the entries of this configurator.
	 * 
	 * @return the updated data object
	 */
	public D getData() {
		return guildConfigData;
	}
	
	<T> Mono<T> getValueFromData(Function<Object, ? extends Mono<T>> valueGetter) {
		return valueGetter.apply(guildConfigData);
	}
	
	<T> void setValueToData(BiFunction<Object, ? super T, Object> valueSetter, T newValue,
			@Nullable Consumer<? super T> valueObserver) {
		for (;;) {
			var oldData = guildConfigData;
			var newData = valueSetter.apply(oldData, newValue);
			if (GUILD_CONFIG_DATA_REF.compareAndSet(this, oldData, newData)) {
				if (valueObserver != null) {
					valueObserver.accept(newValue);
				}
				return;
			}
		}
	}

	public static <D extends GuildConfigData<D>> Builder<D> builder(D guildConfigData) {
		return new Builder<>(guildConfigData);
	}

	public static class Builder<D extends GuildConfigData<D>> {
		
		private final D guildConfigData;
		private final List<ConfigEntryBuilder<D, ?>> addedEntries = new ArrayList<>();
		
		private Builder(D guildConfigData) {
			this.guildConfigData = guildConfigData;
		}
		
		public Builder<D> addEntry(ConfigEntryBuilder<D, ?> configEntryBuilder) {
			addedEntries.add(configEntryBuilder);
			return this;
		}
		
		public GuildConfigurator<D> build() {
			var configEntries = new TreeMap<String, ConfigEntry<?>>();
			var configurator = new GuildConfigurator<>(guildConfigData, configEntries);
			addedEntries.forEach(added -> {
				var configEntry = added.build(configurator);
				configEntries.put(configEntry.getKey(), configEntry);
			});
			return configurator;
		}
	}
}
