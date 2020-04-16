package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public class IntegerConfigEntry extends AbstractConfigEntry<Integer> {

	IntegerConfigEntry(GuildConfigurator<?> configurator, String key, String description,
			Function<Object, ? extends Mono<Integer>> valueGetter,
			BiFunction<Object, ? super Integer, Object> valueSetter, Validator<Integer> validator,
			Consumer<? super Integer> valueObserver) {
		super(configurator, key, description, valueGetter, valueSetter, validator, valueObserver);
	}

	@Override
	public <R> Mono<R> accept(ConfigEntryVisitor<R> visitor) {
		return visitor.visit(this);
	}
	
	/**
	 * Creates a builder for this ConfigEntry implementation.
	 * 
	 * @param <D> the implementation type of {@link GuildConfigData} this entry
	 *            affects
	 * @param key the unique key identifying the built entry
	 * @return a new builder
	 */
	public static <D extends GuildConfigData<D>> ConfigEntryBuilder<D, Integer> builder(String key) {
		return new ConfigEntryBuilder<>(IntegerConfigEntry::new, key);
	}
}
