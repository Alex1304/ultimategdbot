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
public final class PluginMetadata {
	
	private final String name;
	private final String description;
	private final String version;
	private final List<String> developers;
	private final String url;
	
	private PluginMetadata(String name, @Nullable String description, @Nullable String version, @Nullable List<String> developers, @Nullable String url) {
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
	
	/**
	 * Initiates a builder for a {@link PluginMetadata}.
	 * 
	 * @param name the name of the plugin
	 * @return a new {@link Builder}
	 */
	public static Builder builder(String name) {
		return new Builder(name);
	}
	
	/**
	 * Builder to build a {@link PluginMetadata} object.
	 */
	public static class Builder {
		private final String name;
		private String description;
		private String version;
		private List<String> developers;
		private String url;
		
		private Builder(String name) {
			this.name = requireNonNull(name);
		}
		
		/**
		 * Sets the description of the plugin.
		 * 
		 * @param description the description, may be <code>null</code>
		 * @return this builder
		 */
		public Builder setDescription(@Nullable String description) {
			this.description = description;
			return this;
		}

		/**
		 * Sets the version of the plugin.
		 * 
		 * @param version the version, may be <code>null</code>
		 * @return this builder
		 */
		public Builder setVersion(@Nullable String version) {
			this.version = version;
			return this;
		}

		/**
		 * Sets the developers of the plugin.
		 * 
		 * @param developers the developers, may be <code>null</code>
		 * @return this builder
		 */
		public Builder setDevelopers(@Nullable List<String> developers) {
			this.developers = developers;
			return this;
		}

		/**
		 * Sets the url of the plugin.
		 * 
		 * @param url the url, may be <code>null</code>
		 * @return this builder
		 */
		public Builder setUrl(@Nullable String url) {
			this.url = url;
			return this;
		}
		
		/**
		 * Builds the {@link PluginMetadata} object.
		 * 
		 * @return the built {@link PluginMetadata} object
		 */
		public PluginMetadata build() {
			return new PluginMetadata(name, description, version, developers, url);
		}
	}
}
