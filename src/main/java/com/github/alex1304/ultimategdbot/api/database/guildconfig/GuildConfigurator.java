package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.database.DatabaseService;

import discord4j.common.util.Snowflake;
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
public final class GuildConfigurator<D extends GuildConfigData<D>> {
	
	private static final VarHandle GUILD_CONFIG_DATA_REF;
	
	static {
		Lookup lookup = MethodHandles.lookup();
		try {
			GUILD_CONFIG_DATA_REF = lookup.findVarHandle(
					GuildConfigurator.class, "guildConfigData", GuildConfigData.class);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}

	private final String name;
	private final String description;
	private final Map<String, ConfigEntry<?>> configEntries;
	private final Class<? extends GuildConfigDao<D>> daoType;
	private final Consumer<? super D> onSave;

	private volatile D guildConfigData;

	private GuildConfigurator(String name, String description, Map<String, ConfigEntry<?>> configEntries, D initialData,
			Class<? extends GuildConfigDao<D>> daoType, Consumer<? super D> onSave) {
		this.name = name;
		this.description = description;
		this.guildConfigData = initialData;
		this.configEntries = configEntries;
		this.daoType = daoType;
		this.onSave = onSave;
	}
	
	/**
	 * Gets the name of this configurator.
	 * 
	 * @return a String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the description of this configurator.
	 * 
	 * @return a String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets a collection of configuration entries containied in this configurator.
	 * 
	 * @return a collection of configuration entries
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
	
	// For testing
	D getData() {
		return guildConfigData;
	}
	
	/**
	 * Saves in the given database the data object reflecting all modifications
	 * performed through the entries of this configurator.
	 * 
	 * @param database the database where to save data
	 * @return a Mono completing when saving to database is successful
	 */
	public Mono<Void> saveConfig(DatabaseService database) {
		var data = guildConfigData; // volatile read
		return database.useExtension(daoType, dao -> dao.update(data))
				.doOnSuccess(__ -> {
					if (onSave != null) {
						onSave.accept(data);
					}
				});
	}
	
	/**
	 * Resets the configuration to its default value and saves the data with the
	 * default values in the given database.
	 * 
	 * @param database the database where to save the reset data
	 * @return a Mono emitting the data after reset
	 */
	public Mono<D> resetConfig(DatabaseService database) {
		return database.withExtension(daoType, dao -> dao.resetAndGet(guildConfigData.guildId().asLong()))
				.single()
				.doOnNext(resetData -> {
					if (onSave != null) {
						onSave.accept(resetData);
					}
				});
	}
	
	/**
	 * Gets the ID of the guild this configurator is atttached to.
	 * 
	 * @return the guild ID
	 */
	public Snowflake getGuildId() {
		return guildConfigData.guildId();
	}
	
	<T> Mono<T> getValueFromData(Function<Object, ? extends Mono<T>> valueGetter) {
		return valueGetter.apply(guildConfigData);
	}
	
	<T> void setValueToData(BiFunction<Object, ? super T, Object> valueSetter, T newValue) {
		for (;;) {
			var oldData = guildConfigData;
			var newData = valueSetter.apply(oldData, newValue);
			if (GUILD_CONFIG_DATA_REF.compareAndSet(this, oldData, newData)) {
				return;
			}
		}
	}

	/**
	 * Creates a new configurator builder with the given name and initial data.
	 * 
	 * @param <D>         the type of the data object
	 * @param name        the name of the configurator to build
	 * @param initialData the data to initialize the configurator
	 * @param daoType     a daoType class compatible with the data object type
	 * @return a new builder
	 */
	public static <D extends GuildConfigData<D>> Builder<D> builder(String name, D initialData, Class<? extends GuildConfigDao<D>> daoType) {
		requireNonNull(name, "name");
		requireNonNull(initialData, "initialData");
		requireNonNull(daoType, "daoType");
		return new Builder<>(name, initialData, daoType);
	}

	public static class Builder<D extends GuildConfigData<D>> {
		
		private static final String DEFAULT_DESCRIPTION = "";
		
		private final String name;
		private final D initialData;
		private final Class<? extends GuildConfigDao<D>> daoType;
		private String description = DEFAULT_DESCRIPTION;
		private final List<ConfigEntryBuilder<D, ?>> addedEntries = new ArrayList<>();
		private Consumer<? super D> onSave;
		
		private Builder(String name, D initialData, Class<? extends GuildConfigDao<D>> daoType) {
			this.name = name;
			this.initialData = initialData;
			this.daoType = daoType;
		}
		
		/**
		 * Specifies a user-friendly description for this configurator. If not set or is
		 * set to <code>null</code>, an empty string will be used as description.
		 * 
		 * @param description the description to set
		 * @return this builder
		 */
		public Builder<D> setDescription(@Nullable String description) {
			this.description = requireNonNullElse(description, DEFAULT_DESCRIPTION);
			return this;
		}
		
		/**
		 * Adds a new entry to this configurator. The argument is a builder that will
		 * instantiate the entry once the configurator is initialized.
		 * 
		 * @param configEntryBuilder a builder that generates the entry to add
		 * @return this builder
		 */
		public Builder<D> addEntry(ConfigEntryBuilder<D, ?> configEntryBuilder) {
			addedEntries.add(configEntryBuilder);
			return this;
		}
		
		/**
		 * Specifies a callback to invoke when the modified data is saved to the
		 * database.
		 * 
		 * @param onSave a consumer that accepts the data after modification by this
		 *               configurator
		 * @return this builder
		 */
		public Builder<D> onSave(@Nullable Consumer<? super D> onSave) {
			this.onSave = onSave;
			return this;
		}
		
		/**
		 * Builds the configurator with all the attributes previously set.
		 * 
		 * @return a new {@link GuildConfigurator}
		 */
		public GuildConfigurator<D> build() {
			var configEntries = new LinkedHashMap<String, ConfigEntry<?>>();
			var configurator = new GuildConfigurator<>(name, description, configEntries, initialData, daoType, onSave);
			addedEntries.forEach(added -> {
				var configEntry = added.build(configurator);
				configEntries.put(configEntry.getKey(), configEntry);
			});
			return configurator;
		}
	}
}
