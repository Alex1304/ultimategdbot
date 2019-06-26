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
	
	private IllegalArgumentException iae(String reason) {
		return new IllegalArgumentException("Unable to parse property: " + reason);
	}
	
	/**
	 * Parses a value using the given function and returns it
	 * 
	 * @param name   the entry name in the properties file
	 * @param parser the function that parses the String value into the desired type
	 * @param        <P> the type of value the configuration entry is supposed to
	 *               match
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value could not be parsed or
	 *         if the configuration entry associated with {@code name} doesn't
	 *         exist.
	 */
	public <P> P parse(String name, Function<String, P> parser) {
		var prop = props.getProperty(name);
		if (prop == null) {
			throw iae("The property '" + name + "' is missing");
		}
		try {
			return parser.apply(prop);
		} catch (NumberFormatException e) {
			throw iae("The property '" + name + "' was expected to be a numeric value");
		} catch (IllegalArgumentException e) {
			throw iae("Invalid value for property '" + name + "'" + (e.getMessage() != null ? ": " + e.getMessage() : ""));
		}
	}

	/**
	 * Parses a value using the given function and returns it. It is similar to
	 * {@link #parse(String, Function)}, except that it returns a default value
	 * instead of throwing an {@link IllegalArgumentException}
	 * 
	 * @param name   the entry name in the properties file
	 * @param parser the function that parses the String value into the desired type
	 * @param defVal the default value to return if not found or couldn't be parsed
	 * @param        <P> the type of value the configuration entry is supposed to
	 *               match
	 * @return the parsed value
	 */
	public <P> P parseOrDefault(String name, Function<String, P> parser, P defVal) {
		try {
			return parse(name, parser);
		} catch (IllegalArgumentException e) {
			return defVal;
		}
	}
	
	/**
	 * Convenient method that is similar as doing:
	 * <pre>
	 * parse(name, String::toString)
	 * </pre>
	 * 
	 * @param name   the entry name in the properties file
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value could not be parsed or
	 *         if the configuration entry associated with {@code name} doesn't
	 *         exist.
	 * @see #parse(String, Function)
	 */
	public String parseAsString(String name) {
		return parse(name, String::toString);
	}
	
	/**
	 * Convenient method that is similar as doing:
	 * 
	 * <pre>
	 * parseOrDefault(name, String::toString, defVal)
	 * </pre>
	 * 
	 * @param name   the entry name in the properties file
	 * @param defVal the default value to return if not found or couldn't be parsed
	 * @return the parsed value
	 * @see #parseOrDefault(String, Function, Object)
	 */
	public String parseAsStringOrDefault(String name, String defVal) {
		return parseOrDefault(name, String::toString, defVal);
	}

	/**
	 * Convenient method that is similar as doing:
	 * 
	 * <pre>
	 * parse(name, Integer::parseInt)
	 * </pre>
	 * 
	 * @param name the entry name in the properties file
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value could not be parsed or
	 *         if the configuration entry associated with {@code name} doesn't
	 *         exist.
	 * @see #parse(String, Function)
	 */
	public int parseAsInt(String name) {
		return parse(name, Integer::parseInt);
	}
	
	/**
	 * Convenient method that is similar as doing:
	 * 
	 * <pre>
	 * parseOrDefault(name, Integer::parseInt, defVal)
	 * </pre>
	 * 
	 * @param name   the entry name in the properties file
	 * @param defVal the default value to return if not found or couldn't be parsed
	 * @return the parsed value
	 * @see #parseOrDefault(String, Function, Object)
	 */
	public int parseAsIntOrDefault(String name, int defVal) {
		return parseOrDefault(name, Integer::parseInt, defVal);
	}
	
	/**
	 * Convenient method that is similar as doing:
	 * 
	 * <pre>
	 * parse(name, Long::parseLong)
	 * </pre>
	 * 
	 * @param name the entry name in the properties file
	 * @return the parsed value
	 * @throws IllegalArgumentException if the value could not be parsed or
	 *         if the configuration entry associated with {@code name} doesn't
	 *         exist.
	 * @see #parse(String, Function)
	 */
	public long parseAsLong(String name) {
		return parse(name, Long::parseLong);
	}

	/**
	 * Convenient method that is similar as doing:
	 * 
	 * <pre>
	 * parseOrDefault(name, Long::parseLong, defVal)
	 * </pre>
	 * 
	 * @param name   the entry name in the properties file
	 * @param defVal the default value to return if not found or couldn't be parsed
	 * @return the parsed value
	 * @see #parseOrDefault(String, Function, Object)
	 */
	public long parseAsLongOrDefault(String name, long defVal) {
		return parseOrDefault(name, Long::parseLong, defVal);
	}
	
	/**
	 * Attempts to parse the vale as a list of values, each element being separated
	 * with a separator character. Let's say the value of the configuration entry
	 * "foo" is formatted as:
	 * 
	 * <pre>
	 * bar:test:demon:hamburger
	 * </pre>
	 * 
	 * Calling this method this way:
	 * 
	 * <pre>
	 * List&lt;String&gt; l = parseAsList("foo", ":", String::toString);
	 * </pre>
	 * 
	 * would output a List with the following contents:
	 * 
	 * <pre>
	 * ["bar", "test", "demon", "hamburger"]
	 * </pre>
	 * 
	 * @param name                the name of the configuration entry in the
	 *                            properties file
	 * @param separator           the character (or sequence of characters) that
	 *                            separates the elements in the raw string. Note
	 *                            that this is actually a regex as this parameter is
	 *                            directly passed to the
	 *                            {@link String#split(String)} method internally. So
	 *                            characters like $ or | should be properly escaped.
	 * @param singleElementParser the parser function to apply on each element of
	 *                            the list
	 * @param                     <E> the type of the elements of the list
	 * @return the List containing all elements
	 * @throws IllegalArgumentException if at least one element failed to parse, or
	 *                                  if the entry for the given name doesn't
	 *                                  exist.
	 */
	public <E> List<E> parseAsList(String name, String separator, Function<String, E> singleElementParser) {
		return parse(name, value -> {
			var parts = value.split(separator);
			var result = new ArrayList<E>();
			for (var i = 0 ; i < parts.length ; i++) {
				try {
					result.add(singleElementParser.apply(parts[i]));
				} catch (IllegalArgumentException e) {
					throw iae("The element '" + parts[i] + "' is not valid for the array property '" + name + "'");
				}
			}
			return result;
		});
	}
	
	/**
	 * Same as {@link #parseAsList(String, String, Function)} but in case of error
	 * returns an empty list instead of throwing an
	 * {@link IllegalArgumentException}.
	 * 
	 * @param name                the name of the configuration entry in the
	 *                            properties file
	 * @param separator           the character (or sequence of characters) that
	 *                            separates the elements in the raw string. Note
	 *                            that this is actually a regex as this parameter is
	 *                            directly passed to the
	 *                            {@link String#split(String)} method internally. So
	 *                            characters like $ or | should be properly escaped.
	 * @param singleElementParser the parser function to apply on each element of
	 *                            the list
	 * @param                     <E> the type of the elements of the list
	 * @return the List containing all elements, or empty if at least one element
	 *         failed to parse, or if the entry for the given name doesn't exist.
	 * @see #parseAsList(String, String, Function)
	 */
	public <E> List<E> parseAsListOrEmpty(String name, String separator, Function<String, E> singleElementParser) {
		try {
			return parseAsList(name, separator, singleElementParser);
		} catch (IllegalArgumentException e) {
			return List.of();
		}
	}
}
