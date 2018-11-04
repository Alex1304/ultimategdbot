package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.xeustechnologies.jcl.JarClassLoader;

/**
 * Loaded plugins are stored here.
 * 
 * @author Alex1304
 *
 */
public class PluginContainer<T extends Plugin> implements Iterable<T> {
	
	private static PluginContainer<Command> commands = null;
	private static PluginContainer<Service> services = null;

	private final ConcurrentHashMap<String, T> pluginMap;
	private final HashSet<String> enabledPlugins;
	private final ConcurrentHashMap<String, JarClassLoader> pluginClassloaderMap;
	

	private PluginContainer() {
		this.pluginMap = new ConcurrentHashMap<>();
		this.enabledPlugins = new HashSet<>();
		this.pluginClassloaderMap = new ConcurrentHashMap<>();
	}

	/**
	 * Gets the instance of a plugin by its name. Returns null if none was found.
	 * This does not check whether the plugin is enabled or disabled
	 * 
	 * @param pluginName - String
	 * @return T
	 */
	public final T get(String pluginName) {
		return pluginMap.get(Objects.requireNonNull(pluginName));
	}

	/**
	 * Checks whether the given plugin name refers to an existing loaded plugin.
	 * Returns {@code true} if so, {@code false} otherwise.
	 * 
	 * @param pluginName - String
	 * @return boolean
	 */
	public final boolean exists(String pluginName) {
		return pluginMap.contains(Objects.requireNonNull(pluginName));
	}

	/**
	 * Enables the plugin specified by its name. Does nothing and silently fails if
	 * the plugin was not found.
	 * 
	 * @param pluginName - String
	 */
	public final void enable(String pluginName) throws PluginInstallationException {
		if (!exists(Objects.requireNonNull(pluginName))) {
			return;
		}
		pluginMap.get(pluginName).install();
		enabledPlugins.add(pluginName);
	}

	/**
	 * Disables the plugin specified by its name. Does nothing and silently fails if
	 * the plugin was not found.
	 * 
	 * @param pluginName - String
	 */
	public final void disable(String pluginName) {
		if (!exists(Objects.requireNonNull(pluginName))) {
			return;
		}

		enabledPlugins.remove(pluginName);
	}

	/**
	 * Checks whether the given plugin is enabled. Returns {@code true} if so,
	 * {@code false} otherwise.
	 * 
	 * @param pluginName - String
	 * @return boolean
	 */
	public final boolean isEnabled(String pluginName) {
		return enabledPlugins.contains(Objects.requireNonNull(pluginName));
	}

	/**
	 * Gets the instance of a plugin by its name. Returns null if none was found. If
	 * the plugin is not enabled, null is returned as well.
	 * 
	 * @param pluginName - String
	 * @return T
	 */
	public final T getEnabledOrNull(String pluginName) {
		return isEnabled(Objects.requireNonNull(pluginName)) ? get(pluginName) : null;
	}

	/**
	 * Synchronizes the items of this container to match the items loaded by the
	 * given ServiceLoader.
	 * 
	 * @param loader - ServiceLoader
	 */
	public void syncFromLoader(ServiceLoader<T> loader, JarClassLoader classloader) {
		pluginMap.clear();

		for (var plugin : loader) {
			pluginMap.put(plugin.getName(), plugin);
			pluginClassloaderMap.put(plugin.getName(), classloader);
		}

		// Ensures that enabledPlugins doesn't have references to plugins that don't
		// exist anymore
		Iterator<String> enabledIterator = enabledPlugins.iterator();
		while (enabledIterator.hasNext()) {
			var enabled = enabledIterator.next();
			if (!exists(enabled)) {
				enabledIterator.remove();
				if (pluginClassloaderMap.contains(enabled)) {
					var cl = pluginClassloaderMap.get(enabled);
					cl.unloadClass(pluginMap.get(enabled).getClass().getName());
					pluginClassloaderMap.remove(enabled);
				}
			}
		}
	}
	
	/**
	 * Returns a unique instance of the plugin container for commands
	 * 
	 * @return PluginContainer&lt;Command&gt;
	 */
	public static synchronized PluginContainer<Command> ofCommands() {
		if (commands == null) {
			commands = new PluginContainer<>();
		}
		
		return commands;
	}
	
	/**
	 * Returns a unique instance of the plugin container for services
	 * 
	 * @return PluginContainer&lt;Service&gt;
	 */
	public static synchronized PluginContainer<Service> ofServices() {
		if (services == null) {
			services = new PluginContainer<>();
		}
		
		return services;
	}

	@Override
	public Iterator<T> iterator() {
		return pluginMap.values().iterator();
	}
}
