package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

abstract class AbstractConfigEntry<G extends GuildConfigData<G>, T> implements ConfigEntry<G, T> {
	
	private final GuildConfigurator<G> configurator;
	private final String key;
	private final String description;
	private final Function<? super G, ? extends Mono<T>> valueGetter;
	private final BiFunction<? super G, ? super T, ? extends G> valueSetter;
	private final Validator<T> validator;
	
	AbstractConfigEntry(GuildConfigurator<G> configurator, String key, String description, Function<? super G, ? extends Mono<T>> valueGetter,
			BiFunction<? super G, ? super T, ? extends G> valueSetter, Validator<T> validator) {
		this.configurator = configurator;
		this.key = key;
		this.description = description;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
		this.validator = validator;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Mono<T> getValue() {
		return valueGetter.apply(configurator.getData());
	}

	@Override
	public Mono<Void> setValue(@Nullable T newValue) {
		return validator.apply(newValue).flatMap(validatedValue -> Mono.fromRunnable(
				() -> configurator.updateData(data -> valueSetter.apply(data, validatedValue))));
	}
}