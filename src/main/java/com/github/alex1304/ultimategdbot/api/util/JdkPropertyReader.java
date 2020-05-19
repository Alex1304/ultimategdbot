package com.github.alex1304.ultimategdbot.api.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

class JdkPropertyReader implements PropertyReader {
	private final Properties props;
	
	JdkPropertyReader(Properties props) {
		this.props = Objects.requireNonNull(props);
	}
	
	@Override
	public String read(String key) {
		return Optional.ofNullable(props.getProperty(key)).orElseThrow();
	}
	
	@Override
	public <T> T readAs(String key, Function<? super String, ? extends T> mapper) {
		return Optional.ofNullable(props.getProperty(key)).map(mapper).orElseThrow();
	}

	@Override
	public Optional<String> readOptional(String key) {
		return Optional.ofNullable(props.getProperty(key));
	}

	@Override
	public Stream<String> readAsStream(String key, String separator) {
		return Optional.ofNullable(props.getProperty(key)).stream().flatMap(value -> Arrays.stream(value.split(separator)));
	}

	@Override
	public String toString() {
		return "JdkPropertyReader{props=" + props + "}";
	}

	@Override
	public Properties toJdkProperties() {
		return (Properties) props.clone();
	}
}
