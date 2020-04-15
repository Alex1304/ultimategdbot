package com.github.alex1304.ultimategdbot.api.guildconfig;

import static java.util.Objects.requireNonNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public class ConfigEntryBuilder<G extends GuildConfigData<G>, T> {
	
	private final ConfigEntryFactory<G, T> factory;
	private final String key;
	private String description = "";
	private Function<? super G, ? extends Mono<T>> valueGetter = data -> Mono.empty();
	private BiFunction<? super G, ? super T, ? extends G> valueSetter = (data, newValue) -> data;
	private Validator<T> validator = Validator.allowingAll();
	
	ConfigEntryBuilder(ConfigEntryFactory<G, T> constructor, String key) {
		this.factory = constructor;
		this.key = requireNonNull(key);
	}

	public ConfigEntryBuilder<G, T> setDescription(String description) {
		this.description = description;
		return this;
	}

	public ConfigEntryBuilder<G, T> setValueGetter(Function<? super G, ? extends Mono<T>> valueGetter) {
		this.valueGetter = valueGetter;
		return this;
	}

	public ConfigEntryBuilder<G, T> setValueSetter(BiFunction<? super G, ? super T, ? extends G> valueSetter) {
		this.valueSetter = valueSetter;
		return this;
	}

	public ConfigEntryBuilder<G, T> setValidator(Validator<T> validator) {
		this.validator = validator;
		return this;
	}
	
	ConfigEntry<G, T> build(GuildConfigurator<G> configurator) {
		return factory.create(configurator, key, description, valueGetter, valueSetter, validator);
	}
	
	static interface ConfigEntryFactory<G extends GuildConfigData<G>, T> {
		ConfigEntry<G, T> create(GuildConfigurator<G> configurator, String key, String description, Function<? super G, ? extends Mono<T>> valueGetter,
			BiFunction<? super G, ? super T, ? extends G> valueSetter, Validator<T> validator);
	}
}