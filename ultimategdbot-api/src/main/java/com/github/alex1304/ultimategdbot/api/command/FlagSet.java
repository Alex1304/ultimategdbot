package com.github.alex1304.ultimategdbot.api.command;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Contains the set of flags used in a command. A flag is defined by a name and
 * an optional value.
 */
public class FlagSet {
	
	private final Map<String, Optional<String>> flagMap;
	
	public FlagSet(Map<String, Optional<String>> flagMap) {
		this.flagMap = flagMap;
	}

	public static class FlagSetBuilder {
		private final Map<String, Optional<String>> flagMap = new HashMap<>();
		
		private FlagSetBuilder() {
		}
		
		public void add(String name, String value) {
			flagMap.put(name, Optional.ofNullable(value));
		}
		
		public void add(String name) {
			add(name, null);
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
	 * Checks whether the flag with he given name is present in this set.
	 * 
	 * @param name the name of the flag to look for
	 * @return true if present in the set, false otherwise
	 */
	public boolean has(String name) {
		return flagMap.containsKey(name);
	}

	/**
	 * Gets the value of the flag with the given name. If the flag has no value, an
	 * empty Optional is returned. If the flag is not present at all,
	 * {@link NoSuchElementException} will be thrown.
	 * 
	 * @param name the name of the flag to look for
	 * @return the value of the flag, or empty optional if no value
	 * @throws NoSuchElementException if the flag is not present at all
	 */
	public Optional<String> get(String name) {
		if (!has(name)) {
			throw new NoSuchElementException();
		}
		return flagMap.get(name);
	}
	
	/**
	 * Gets the value of the flag with the given name, and transforms the value
	 * using the supplied function.
	 * 
	 * @param name   the name of the flag to look for
	 * @param parser the transformation function of the value
	 * @return the transformed value, or empty if no value
	 * @throws NoSuchElementException if the flag is not present at all
	 */
	public <T> Optional<T> parseAndGet(String name, Function<String, T> parser) {
		if (!has(name)) {
			throw new NoSuchElementException();
		}
		return flagMap.get(name).map(parser::apply);
	}
	
	@Override
	public String toString() {
		return "Flags{flagMap=" + flagMap + "}";
	}
}