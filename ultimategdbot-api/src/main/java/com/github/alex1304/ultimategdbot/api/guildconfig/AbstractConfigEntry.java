package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

abstract class AbstractConfigEntry<T> implements ConfigEntry<T> {

	private final GuildConfigurator<?> configurator;
	private final String key;
	private final String description;
	private final Function<Object, ? extends Mono<T>> valueGetter;
	private final BiFunction<Object, ? super T, Object> valueSetter;
	private final Validator<T> validator;
	private final Consumer<? super T> valueObserver;
	
	AbstractConfigEntry(GuildConfigurator<?> configurator, String key, String description, Function<Object, ? extends Mono<T>> valueGetter,
			BiFunction<Object, ? super T, Object> valueSetter, Validator<T> validator, Consumer<? super T> valueObserver) {
		this.configurator = configurator;
		this.key = key;
		this.description = description;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
		this.validator = validator;
		this.valueObserver = valueObserver;
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
	public boolean isReadOnly() {
		return valueSetter == null;
	}

	@Override
	public Mono<T> getValue() {
		return configurator.getValueFromData(valueGetter);
	}

	@Override
	public Mono<Void> setValue(@Nullable T newValue) {
		if (valueSetter == null) {
			return Mono.error(new ReadOnlyConfigEntryException(key));
		}
		return validator.apply(newValue)
				.doOnNext(validatedValue -> configurator.setValueToData(valueSetter, validatedValue, valueObserver))
				.then();
	}
	
	interface Constructor<T> {
		ConfigEntry<T> newInstance(GuildConfigurator<?> configurator, String key, String description, Function<Object, ? extends Mono<T>> valueGetter,
				BiFunction<Object, ? super T, Object> valueSetter, Validator<T> validator, Consumer<? super T> valueObserver);
	}
}