package com.github.alex1304.ultimategdbot.api.command;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

public class Flags {
	
	private final Map<String, Optional<String>> flagMap;
	
	public Flags(Map<String, Optional<String>> flagMap) {
		this.flagMap = flagMap;
	}

	public static class FlagBuilder {
		private final Map<String, Optional<String>> flagMap = new HashMap<>();
		
		private FlagBuilder() {
		}
		
		public void add(String name, String value) {
			flagMap.put(name, Optional.ofNullable(value));
		}
		
		public void add(String name) {
			add(name, null);
		}
		
		public Flags build() {
			return new Flags(flagMap);
		}
	}
	
	public static FlagBuilder builder() {
		return new FlagBuilder();
	}
	
	public boolean has(String name) {
		return flagMap.containsKey(name);
	}
	
	public Optional<String> get(String name) {
		if (!has(name)) {
			throw new NoSuchElementException();
		}
		return flagMap.get(name);
	}
	
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