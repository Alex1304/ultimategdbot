package com.github.alex1304.ultimategdbot.api.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Allows to read values from properties files
 */
public class PropertyReader {
	private final Properties props;
	
	public PropertyReader(Properties props) {
		this.props = Objects.requireNonNull(props);
	}
	
	/**
	 * Reads a value associated witht the given key.
	 * 
	 * @param key      the configuration key
	 * @param required whether the property is required to be present
	 * @return the value associated to the key, if present
	 * @throws IllegalArgumentException if required is true and the property is
	 *                                  missing
	 */
	public Optional<String> read(String key, boolean required) {
		var value = props.getProperty(key);
		if (required && value == null) {
			throw new IllegalArgumentException("Missing property: " + key);
		}
		return Optional.ofNullable(value);
	}

	/**
	 * Reads the value as a stream of values, each element being separated with a
	 * separator character. Let's say the value of the configuration entry "foo" is
	 * formatted as:
	 * 
	 * <pre>
	 * bar:test:demon:hamburger
	 * </pre>
	 * 
	 * Calling this method this way:
	 * 
	 * <pre>
	 * readAsStream("foo", ":", String::toString);
	 * </pre>
	 * 
	 * would return a Stream with the following elements:
	 * 
	 * <pre>
	 * ["bar", "test", "demon", "hamburger"]
	 * </pre>
	 * 
	 * @param name      the name of the configuration entry in the properties file
	 * @param separator the character (or sequence of characters) that separates the
	 *                  elements in the raw string. Note that this is actually a
	 *                  regex as this parameter is directly passed to the
	 *                  {@link String#split(String)} method internally. So
	 *                  characters like $ or | should be properly escaped
	 * @return a Stream containing all elements
	 */
	public Stream<String> readAsStream(String name, String separator) {
		return Optional.ofNullable(props.getProperty(name)).stream().flatMap(value -> Arrays.stream(value.split(separator)));
	}
}
