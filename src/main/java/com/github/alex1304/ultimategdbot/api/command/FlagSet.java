package com.github.alex1304.ultimategdbot.api.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contains the set of flags used in a command. A flag is defined by a name and
 * an optional value.
 */
public final class FlagSet {
	
	private final Map<String, Optional<String>> flagMap;
	
	public FlagSet(Map<String, Optional<String>> flagMap) {
		this.flagMap = flagMap;
	}

	public static class FlagSetBuilder {
		private final Map<String, Optional<String>> flagMap = new HashMap<>();
		
		private FlagSetBuilder() {
		}
		
		public void add(String name, String value) {
			flagMap.put(name, Optional.of(value));
		}
		
		public void add(String name) {
			add(name, "");
		}
		
		public FlagSet build() {
			return new FlagSet(flagMap);
		}
	}
	
	/**
	 * Creates a builder to configure a new set of flags.
	 * 
	 * @return a flag builder
	 */
	public static FlagSetBuilder builder() {
		return new FlagSetBuilder();
	}

	/**
	 * Gets the value of the flag with the given name. If the flag has no value, the
	 * value is an empty string. If the flag is not present at all, and empty
	 * Optional is returned.
	 * 
	 * @param name the name of the flag to look for
	 * @return the value of the flag, or empty optional if flag is not present
	 */
	public Optional<String> get(String name) {
		return flagMap.getOrDefault(name, Optional.empty());
	}
	
	@Override
	public String toString() {
		return "Flags{flagMap=" + flagMap + "}";
	}
}