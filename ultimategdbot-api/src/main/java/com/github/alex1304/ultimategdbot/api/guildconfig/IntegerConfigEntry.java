package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public class IntegerConfigEntry<G extends GuildConfigData<G>> extends AbstractConfigEntry<G, Integer> {

	IntegerConfigEntry(GuildConfigurator<G> configurator, String key, String description,
			Function<? super G, ? extends Mono<Integer>> valueGetter,
			BiFunction<? super G, ? super Integer, ? extends G> valueSetter, Validator<Integer> validator) {
		super(configurator, key, description, valueGetter, valueSetter, validator);
	}

	@Override
	public <R> Mono<R> accept(ConfigEntryVisitor<G, R> visitor) {
		return visitor.visit(this);
	}
	
	/**
	 * Creates a builder for this ConfigEntry implementation.
	 * 
	 * @param <G> the implementation type of {@link GuildConfigData} this entry
	 *            affects
	 * @param key the unique key identifying the built entry
	 * @return a new builder
	 */
	public static <G extends GuildConfigData<G>> ConfigEntryBuilder<G, Integer> builder(String key) {
		return new ConfigEntryBuilder<>(IntegerConfigEntry::new, key);
	}
}
