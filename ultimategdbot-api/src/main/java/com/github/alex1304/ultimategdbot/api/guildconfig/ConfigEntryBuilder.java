package com.github.alex1304.ultimategdbot.api.guildconfig;

import static java.util.Objects.requireNonNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;

/**
 * Provides methods to build a configuration entry.
 * 
 * @param <D> the type of data object storing the value of the entry being built
 * @param <T> the type of value supported by the entry being built
 */
public class ConfigEntryBuilder<D extends GuildConfigData<D>, T> {
	
	private final AbstractConfigEntry.Constructor<T> constructor;
	private final String key;
	private String description = "";
	private Function<? super D, ? extends Mono<T>> valueGetter = data -> Mono.empty();
	private BiFunction<? super D, ? super T, ? extends D> valueSetter;
	private Validator<T> validator = Validator.allowingAll();
	
	ConfigEntryBuilder(AbstractConfigEntry.Constructor<T> constructor, String key) {
		this.constructor = constructor;
		this.key = requireNonNull(key);
	}

	/**
	 * Specifies a user-friendly description for this entry. If not set, an empty
	 * string will be used as description.
	 * 
	 * @param description the description to set
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setDescription(String description) {
		this.description = requireNonNull(description);
		return this;
	}

	/**
	 * Specifies how to extract the value from the data object. If no value is
	 * present (e.g null in database), an empty {@link Mono} should be returned. If
	 * not set, the function will always return an empty {@link Mono}.
	 * 
	 * @param valueGetter the function that extracts and returns the value from the
	 *                    data object with asynchronous capabilities
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValueGetter(Function<? super D, ? extends Mono<T>> valueGetter) {
		this.valueGetter = requireNonNull(valueGetter);
		return this;
	}

	/**
	 * Specifies how to update the value and store it in the data object. 
	 * 
	 * @param valueSetter the bifunction that updates the value and stores it in the
	 *                    data object, then returns the mutated data object (might
	 *                    as well return a new instance if the data object is
	 *                    immutable)
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValueSetter(BiFunction<? super D, ? super T, ? extends D> valueSetter) {
		this.valueSetter = requireNonNull(valueSetter);
		return this;
	}

	/**
	 * Sets the validator that will validate new values set to the entry.
	 * 
	 * @param validator the validator to set
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValidator(Validator<T> validator) {
		this.validator = requireNonNull(validator);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	ConfigEntry<T> build(GuildConfigurator<D> configurator) {
		return constructor.newInstance(configurator, key, description, o -> valueGetter.apply((D) o),
				valueSetter == null ? null : (o, v) -> valueSetter.apply((D) o, v), validator);
	}
}