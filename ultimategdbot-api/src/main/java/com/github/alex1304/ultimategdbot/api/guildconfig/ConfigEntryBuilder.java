package com.github.alex1304.ultimategdbot.api.guildconfig;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * Provides methods to build a configuration entry.
 * 
 * @param <D> the type of data object storing the value of the entry being built
 * @param <T> the type of value supported by the entry being built
 */
public class ConfigEntryBuilder<D extends GuildConfigData<D>, T> {
	
	private static final String DEFAULT_DESCRIPTION = "";
	
	private final AbstractConfigEntry.Constructor<T> constructor;
	private final String key;
	private String description = "";
	private Function<? super D, ? extends Mono<T>> valueGetter = this::defaultGetter;
	private BiFunction<? super D, ? super T, ? extends D> valueSetter;
	private Validator<T> validator = Validator.allowingAll();
	private Consumer<? super T> valueObserver;
	
	ConfigEntryBuilder(AbstractConfigEntry.Constructor<T> constructor, String key) {
		this.constructor = constructor;
		this.key = requireNonNull(key);
	}

	/**
	 * Specifies a user-friendly description for this entry. If not set or is set to
	 * <code>null</code>, an empty string will be used as description.
	 * 
	 * @param description the description to set
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setDescription(@Nullable String description) {
		this.description = requireNonNullElse(description, DEFAULT_DESCRIPTION);
		return this;
	}

	/**
	 * Specifies how to extract the value from the data object. If no value is
	 * present (e.g null in database), an empty {@link Mono} should be returned. If
	 * not set or is set to <code>null</code>, the function will always return an
	 * empty {@link Mono}.
	 * 
	 * @param valueGetter the function that extracts and returns the value from the
	 *                    data object with asynchronous capabilities
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValueGetter(@Nullable Function<? super D, ? extends Mono<T>> valueGetter) {
		// For some f***ing reason requireNonNullElse doesn't work here...
		this.valueGetter = valueGetter == null ? this::defaultGetter : valueGetter;
		return this;
	}

	/**
	 * Specifies how to update the value and store it in the data object. The
	 * function <b>MUST NOT</b> have any side-effects, i.e should not do anything
	 * other than setting the value. Since configurators and entries are thread-safe
	 * and atomically set new values in a lock-free way, the setter is prone to be
	 * called more than once at each value update. If you want to add a side-effect
	 * when a new value is set, see {@link #setValueObserver(Consumer)}.
	 * 
	 * <p>
	 * If nothing is set or is set to <code>null</code>, the entry will be marked as
	 * read-only and any attempt to modify the value will fail
	 * </p>
	 * 
	 * @param valueSetter the bifunction that updates the value and stores it in the
	 *                    data object, then returns the mutated data object (might
	 *                    as well return a new instance if the data object is
	 *                    immutable)
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValueSetter(
			@Nullable BiFunction<? super D, ? super T, ? extends D> valueSetter) {
		this.valueSetter = requireNonNull(valueSetter);
		return this;
	}

	/**
	 * Specifies the validator that will validate new values set to the entry.
	 * 
	 * @param validator the validator to set
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValidator(Validator<T> validator) {
		this.validator = requireNonNull(validator);
		return this;
	}
	
	/**
	 * Specifies a callback to invoke when a new value for this entry is set.
	 * 
	 * @param valueObserver a {@link Consumer} accepting the value set
	 * @return this builder
	 */
	public ConfigEntryBuilder<D, T> setValueObserver(Consumer<? super T> valueObserver) {
		this.valueObserver = requireNonNull(valueObserver);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	ConfigEntry<T> build(GuildConfigurator<D> configurator) {
		return constructor.newInstance(configurator, key, description, o -> valueGetter.apply((D) o),
				valueSetter == null ? null : (o, v) -> valueSetter.apply((D) o, v), validator,
				valueObserver);
	}
	
	private Mono<T> defaultGetter(Object data) {
		return Mono.empty();
	}
}