package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public class BooleanConfigEntry extends AbstractConfigEntry<Boolean> {

	BooleanConfigEntry(GuildConfigurator<?> configurator, String displayName, String key, String prompt,
			Function<Object, ? extends Mono<Boolean>> valueGetter,
			BiFunction<Object, ? super Boolean, Object> valueSetter, Validator<Boolean> validator) {
		super(configurator, displayName, key, prompt, valueGetter, valueSetter, validator);
		// TODO Auto-generated constructor stub
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
	public static <D extends GuildConfigData<D>> ConfigEntryBuilder<D, Boolean> builder(String key) {
		return new ConfigEntryBuilder<>(BooleanConfigEntry::new, key);
	}
}
