package com.github.alex1304.ultimategdbot.api.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Allows to read values from an abstracted source of properties.
 */
public interface PropertyReader {
	
	/**
	 * A {@link PropertyReader} that doesn't contain any property.
	 */
	static final PropertyReader EMPTY = new PropertyReader() {
		@Override
		public String read(String key) {
			throw new NoSuchElementException();
		}
		
		@Override
		public <T> T readAs(String key, Function<? super String, ? extends T> mapper) {
			throw new NoSuchElementException();
		}
		
		@Override
		public Optional<String> readOptional(String key) {
			return Optional.empty();
		}
		
		@Override
		public Stream<String> readAsStream(String name, String separator) {
			return Stream.empty();
		}

		@Override
		public Properties toJdkProperties() {
			return new Properties();
		}
	};

	/**
	 * Reads a value associated with the given key. If no value is present,
	 * {@link NoSuchElementException} is thrown.
	 * 
	 * @param key the configuration key
	 * @return the value associated with the key,
	 * @throws NoSuchElementException if no value is present
	 */
	String read(String key);

	/**
	 * Reads the value associated with the given key and maps it to an object via
	 * the given function. If no value is present, {@link NoSuchElementException} is
	 * thrown.
	 * 
	 * @param <T>    the type of object to map the value to
	 * @param key    the configuration key
	 * @param mapper the function that maps the read value into an object
	 * @return the object produced from the read value
	 */
	<T> T readAs(String key, Function<? super String, ? extends T> mapper);

	/**
	 * Reads a value associated with the given key, if present.
	 * 
	 * @param key the configuration key
	 * @return the value associated with the key, if present
	 */
	Optional<String> readOptional(String key);

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
	 * @param key       the configuration key
	 * @param separator the character (or sequence of characters) that separates the
	 *                  elements in the raw string. Note that this is actually a
	 *                  regex as this parameter is directly passed to the
	 *                  {@link String#split(String)} method internally. So
	 *                  characters like $ or | should be properly escaped
	 * @return a Stream containing all elements
	 */
	Stream<String> readAsStream(String key, String separator);
	
	/**
	 * Converts this {@link PropertyReader} into a JDK {@link Properties} object.
	 * Modifications to the properties object will not be reflected to this property
	 * reader.
	 * 
	 * @return a {@link Properties} object containing same data as this
	 *         {@link PropertyReader}.
	 */
	Properties toJdkProperties();

	/**
	 * Creates a new {@link PropertyReader} reading properties from the given
	 * {@link Properties} object.
	 * 
	 * @param props the {@link Properties} object to read
	 * @return a new {@link PropertyReader}
	 */
	static PropertyReader fromProperties(Properties props) {
		return new JdkPropertyReader(props);
	}
	
	/**
	 * Reads the properties from a Java properties file located at the given
	 * {@link Path}.
	 * 
	 * @param path the path where the properties file is located
	 * @return a Mono emitting a PropertyReader backed by the properties read from
	 *         file
	 */
	static Mono<PropertyReader> fromPropertiesFile(Path path) {
		return Mono.fromCallable(() -> {
			try (var input = Files.newBufferedReader(path)) {
				var props = new Properties();
				props.load(input);
				return PropertyReader.fromProperties(props);
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}
}
