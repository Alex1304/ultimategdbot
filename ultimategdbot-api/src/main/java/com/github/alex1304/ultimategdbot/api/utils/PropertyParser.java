package com.github.alex1304.ultimategdbot.api.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * Allows to parse values from properties files.
 */
public class PropertyParser {
	
	private final Properties props;
	
	public PropertyParser(Properties props) {
		this.props = Objects.requireNonNull(props);
	}
	
	public <P> P parse(String name, Function<String, P> parser) {
		var prop = props.getProperty(name);
		if (prop == null) {
			throw new IllegalArgumentException("The property '" + name + "' is missing");
		}
		try {
			return parser.apply(prop);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("The property '" + name + "' was expected to be a numeric value");
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid value for property '" + name + "'" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
		}
	}
	
	public String parseAsString(String name) {
		return parse(name, String::toString);
	}
	
	public int parseAsInt(String name) {
		return parse(name, Integer::parseInt);
	}
	
	public long parseAsLong(String name) {
		return parse(name, Long::parseLong);
	}
	
	public <E> List<E> parseAsList(String name, String separator, Function<String, E> singleElementParser) {
		return parse(name, value -> {
			var parts = value.split(separator);
			var result = new ArrayList<E>();
			for (var i = 0 ; i < parts.length ; i++) {
				try {
					result.add(singleElementParser.apply(parts[i]));
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("The element '" + parts[i] + "' is not valid for the array property '" + name + "'");
				}
			}
			return result;
		});
	}
}
