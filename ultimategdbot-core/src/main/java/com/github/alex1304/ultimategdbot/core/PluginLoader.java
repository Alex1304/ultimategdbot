package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.ServiceLoader;

import org.xeustechnologies.jcl.JarClassLoader;

import com.github.alex1304.ultimategdbot.plugin.api.Plugin;
import com.github.alex1304.ultimategdbot.plugin.api.PluginContainer;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;

/**
 * Allows to load plugins when the bot is running. A plugin is a JAR file that
 * contains implementations of services that the bot can execute. Currently
 * there are two types of plugins:
 * <ul>
 * <li>Commands : code executed when a Discord user uses a bot command</li>
 * <li>Services : code executed on bot startup, that commands can depend of</li>
 * </ul>
 * 
 * If a command depends on a service, both the command and the service
 * implementation should be in the same JAR package. Commands that depend on
 * third-party services outside the commands' JAR are not supported at this
 * moment.
 * 
 * @author Alex1304
 *
 * @param <T> - The class of the interface that the plugins must implement in
 *        order to be loaded
 */
abstract class PluginLoader<T extends Plugin> {
	
	static final String DEFAULT_PLUGIN_DIR = "./plugins/";

	private final String pluginDirectory;
	private final Class<T> serviceClass;

	PluginLoader(String pluginDirectory, Class<T> serviceClass) {
		this.pluginDirectory = Objects.requireNonNull(pluginDirectory);
		this.serviceClass = Objects.requireNonNull(serviceClass);
	}

	/**
	 * Loads the plugins located in the directory specified when instanciating the
	 * plugin loader. Any previously loaded plugin will be removed if the plugin
	 * could not be found during this call. The plugin instances are stored in the
	 * given pluginContainer object. This method is thread-safe.
	 * 
	 * @param pluginContainer - PluginContainer
	 */
	final synchronized void loadInto(PluginContainer<T> pluginContainer) {
		var classloader = new JarClassLoader();
		classloader.add(pluginDirectory);
		pluginContainer.syncFromLoader(ServiceLoader.load(serviceClass, classloader), classloader);
	}

	/**
	 * Defines the way that the bot should execute code loaded from plugins.
	 */
	abstract void bind(UltimateGDBot bot);
}
