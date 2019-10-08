package com.github.alex1304.ultimategdbot.api;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.database.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Represents a plugin. A plugin has a name and provides a list of commands.
 */
public interface Plugin {
	/**
	 * Code executed when the plugin is loaded. This allows the plugin to perform
	 * additional configuration. Emittiong an error here will cancel
	 * the loading of this plugin and will display a warning in the standard output.
	 * Other plugins won't be affected.
	 * 
	 * @param bot    the bot instance
	 * @param parser contains everything defined in plugins.properties, ready to be
	 *               parsed
	 */
	Mono<Void> setup(Bot bot, PropertyParser parser);
	
	/**
	 * Action to execute when the bot is ready. Errors emitted from here will be
	 * logged on the WARN level then suppressed.
	 * 
	 * @param bot the bot instance
	 * 
	 * @return a Mono that completes when the action is finished
	 */
	default Mono<Void> onBotReady(Bot bot) {
		return Mono.empty();
	}

	/**
	 * Gets the name of the plugin.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Gets a set of resource names that corresponds to database mapping files.
	 * Mapping files usually end with *.hbm.xml and should be located at the root of
	 * the {@code src/main/resources} directory of the plugin
	 * 
	 * @return a set containing the name of all mapping files used in the plugin.
	 */
	Set<String> getDatabaseMappingResources();

	/**
	 * Gets a map of configuration entries for guilds. Anything added here will be
	 * listed when using the setup command.
	 * 
	 * @return the guild configuration entries
	 */
	Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries();
	
	/**
	 * Gets the command provider for this plugin.
	 * 
	 * @return the command provider
	 */
	CommandProvider getCommandProvider();
	
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
	default Mono<Properties> getGitProperties() {
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
}
