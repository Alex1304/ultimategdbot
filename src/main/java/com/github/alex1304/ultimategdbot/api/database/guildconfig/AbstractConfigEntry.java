package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

abstract class AbstractConfigEntry<T> implements ConfigEntry<T> {

	private final GuildConfigurator<?> configurator;
	private final String displayName;
	private final String description;
	private final String key;
	private final Function<Object, ? extends Mono<T>> valueGetter;
	private final BiFunction<Object, ? super T, Object> valueSetter;
	private final Validator<T> validator;
	
	AbstractConfigEntry(GuildConfigurator<?> configurator, String displayName, String description, String key,
			Function<Object, ? extends Mono<T>> valueGetter,
			BiFunction<Object, ? super T, Object> valueSetter, Validator<T> validator) {
		this.configurator = configurator;
		this.displayName = displayName;
		this.description = description;
		this.key = key;
		this.valueGetter = valueGetter;
		this.valueSetter = valueSetter;
		this.validator = validator;
	}
	
	@Override
	public Snowflake getGuildId() {
		return configurator.getGuildId();
	}
	
	@Override
	public String getDisplayName() {
		return displayName;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getKey() {
		return key;
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
				.doOnNext(validatedValue -> configurator.setValueToData(valueSetter, validatedValue))
				.switchIfEmpty(Mono.fromRunnable(() -> configurator.setValueToData(valueSetter, null)))
				.then();
	}
	
	interface Constructor<T> {
		ConfigEntry<T> newInstance(GuildConfigurator<?> configurator, String displayName, String description,
				String key, Function<Object, ? extends Mono<T>> valueGetter,
				BiFunction<Object, ? super T, Object> valueSetter, Validator<T> validator);
	}
}