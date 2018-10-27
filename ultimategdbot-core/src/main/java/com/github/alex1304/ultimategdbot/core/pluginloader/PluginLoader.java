package com.github.alex1304.ultimategdbot.core.pluginloader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import org.xeustechnologies.jcl.JarClassLoader;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

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
public abstract class PluginLoader<T> implements Iterable<T> {

	private final String pluginDirectory;
	private final JarClassLoader classloader;
	private final ServiceLoader<T> serviceloader;

	public PluginLoader(String pluginDirectory, Class<T> serviceClass) {
		this.pluginDirectory = Objects.requireNonNull(pluginDirectory);
		this.classloader = new JarClassLoader();
		this.serviceloader = ServiceLoader.load(serviceClass, classloader);
	}

	public void load() {
		var loadedClassesCopy = new HashMap<>(classloader.getLoadedClasses());

		for (var entry : loadedClassesCopy.entrySet()) {
			classloader.unloadClass(entry.getKey());
		}

		classloader.add(pluginDirectory);
		serviceloader.reload();
	}

	@Override
	public final Iterator<T> iterator() {
		return serviceloader.iterator();
	}

	public final Stream<T> stream() {
		return serviceloader.stream().map(p -> p.get());
	}

	/**
	 * Defines the way that the bot should execute code loaded from plugins
	 */
	public abstract void bindPluginsToBot(UltimateGDBot bot);
}
