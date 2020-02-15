package com.github.alex1304.ultimategdbot.api;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.util.BotUtils;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Represents a plugin. A plugin has a name and provides commands.
 */
public class Plugin {
	
	private final String name;
	private final Set<String> databaseMappingResources;
	private final Map<String, GuildSettingsEntry<?, ?>> guildSettingsEntries;
	private final CommandProvider commandProvider;
	
	private Plugin(String name, Set<String> databaseMappingResources, Map<String, GuildSettingsEntry<?, ?>> guildSettingsEntries,
			CommandProvider commandProvider) {
		this.name = name;
		this.databaseMappingResources = databaseMappingResources;
		this.guildSettingsEntries = guildSettingsEntries;
		this.commandProvider = commandProvider;
	}

	/**
	 * Gets the name of the plugin.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets a set of resource names that corresponds to database mapping files.
	 * Mapping files usually end with *.hbm.xml and should be located at the root of
	 * the {@code src/main/resources} directory of the plugin
	 * 
	 * @return a set containing the name of all mapping files used in the plugin.
	 */
	public Set<String> getDatabaseMappingResources() {
		return unmodifiableSet(databaseMappingResources);
	}

	/**
	 * Gets a map of configuration entries for guilds. Anything added here will be
	 * listed when using the setup command.
	 * 
	 * @return the guild configuration entries
	 */
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries() {
		return unmodifiableMap(guildSettingsEntries);
	}
	
	/**
	 * Gets the command provider for this plugin.
	 * 
	 * @return the command provider
	 */
	public CommandProvider getCommandProvider() {
		return commandProvider;
	}
	
	/**
	 * Gets the Git properties for this plugin. By default, it will look for a file
	 * named <code>[plugin name].git.properties</code> (where plugin name is the
	 * name of the plugin as returned by {@link #getName()} but all lowercase and
	 * with spaces replaced with underscores), in the <code>gitprops/</code>
	 * subdirectory of the resource classpath. If none is found, the returned Mono
	 * will complete empty.
	 * 
	 * @return a Mono emitting the git properties if found
	 */
	public Mono<Properties> getGitProperties() {
		return Mono.fromCallable(() -> {
			var props = new Properties();
			try (var stream = BotUtils.class
					.getResourceAsStream("/gitprops/" + getName().toLowerCase().replace(' ', '_') + ".git.properties")) {
				if (stream != null) {
					props.load(stream);
				}
			} catch (IOException e) {
				
			}
			return props;
		}).subscribeOn(Schedulers.elastic());
	}
	
	/**
	 * Creates a new plugin builder with the specified name.
	 * 
	 * @param name the name of the plugin to build
	 * @return a new Builder
	 */
	public static Builder builder(String name) {
		return new Builder(name);
	}
	
	public static class Builder {
		
		private final String name;
		private final Set<String> databaseMappingResources = new HashSet<>();
		private final Map<String, GuildSettingsEntry<?, ?>> guildSettingsEntries = new LinkedHashMap<>();
		private CommandProvider commandProvider = new CommandProvider();
		
		private Builder(String name) {
			this.name = requireNonNull(name);
		}
		
		/**
		 * Adds one or more resource paths where database mapping files are located.
		 * 
		 * @param resource the first resource
		 * @param more     other resources
		 * @return this builder
		 */
		public Builder addDatabaseMappingRessources(String resource, String... more) {
			requireNonNull(resource);
			requireNonNull(more);
			databaseMappingResources.add(resource);
			databaseMappingResources.addAll(Arrays.asList(more));
			return this;
		}
		
		/**
		 * Adds a new entry to guild settings.
		 * 
		 * @param key   the key of the entry
		 * @param entry the entry itself
		 * @return this builder
		 */
		public Builder addGuildSettingsEntry(String key, GuildSettingsEntry<?, ?> entry) {
			requireNonNull(key);
			requireNonNull(entry);
			guildSettingsEntries.put(key, entry);
			return this;
		}
		
		/**
		 * Sets the command provider for this plugin.
		 * 
		 * @param commandProvider the command provider
		 * @return this builder
		 */
		public Builder setCommandProvider(CommandProvider commandProvider) {
			this.commandProvider = requireNonNull(commandProvider);
			return this;
		}
		
		/**
		 * Builds the plugin instance.
		 * 
		 * @return the plugin instance
		 */
		public Plugin build() {
			return new Plugin(name, databaseMappingResources, guildSettingsEntries, commandProvider);
		}
	}
}
