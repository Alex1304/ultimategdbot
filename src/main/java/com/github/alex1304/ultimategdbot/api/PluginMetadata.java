package com.github.alex1304.ultimategdbot.api;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import reactor.util.annotation.Nullable;

/**
 * Holds metadata describing a plugin.
 */
public class PluginMetadata {
	
	private final String name;
	private final String description;
	private final String version;
	private final List<String> developers;
	private final String url;
	
	/**
	 * Creates a metadata object. Everything may be null except the name. If no name should be provided, it is recommended not to create any metadata object at all.
	 * @param name the name of the plugin, cannot be null
	 * @param description the description of the plugin, may be null
	 * @param version the version of the plugin, may be null
	 * @param developers the list of developers that contributed to this plugin, may be null
	 * @param url the URL of the plugin's site of repository, may be null
	 */
	public PluginMetadata(String name, @Nullable String description, @Nullable String version, @Nullable List<String> developers, @Nullable String url) {
		this.name = requireNonNull(name, "name");
		this.description = description;
		this.version = version;
		this.developers = requireNonNullElse(developers, List.of());
		this.url = url;
	}

	/**
	 * Gets the name of the plugin
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the description of the plugin
	 * 
	 * @return the description
	 */
	public Optional<String> getDescription() {
		return Optional.ofNullable(description);
	}

	/**
	 * Gets the version of the plugin
	 * 
	 * @return the version
	 */
	public Optional<String> getVersion() {
		return Optional.ofNullable(version);
	}

	/**
	 * Gets the list of developerrs that contributed to this plugin
	 * 
	 * @return the list of developers
	 */
	public List<String> getDevelopers() {
		return Collections.unmodifiableList(developers);
	}

	/**
	 * Gets the URL of the plugin's site or repository.
	 * 
	 * @return the url
	 */
	public Optional<String> getUrl() {
		return Optional.ofNullable(url);
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, developers, name, url, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PluginMetadata))
			return false;
		PluginMetadata other = (PluginMetadata) obj;
		return Objects.equals(description, other.description) && Objects.equals(developers, other.developers)
				&& Objects.equals(name, other.name) && Objects.equals(url, other.url)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "PluginMetadata{name=" + name + ", description=" + description + ", version=" + version + ", developers="
				+ developers + ", url=" + url + "}";
	}
}
