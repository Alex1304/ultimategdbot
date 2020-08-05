package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import reactor.core.publisher.Mono;

public final class StringConfigEntry extends AbstractConfigEntry<String> {

	StringConfigEntry(GuildConfigurator<?> configurator, String displayName, String description, String key,
			Function<Object, ? extends Mono<String>> valueGetter,
			BiFunction<Object, ? super String, Object> valueSetter, Validator<String> validator) {
		super(configurator, displayName, description, key, valueGetter, valueSetter, validator);
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
	public static <D extends GuildConfigData<D>> ConfigEntryBuilder<D, String> builder(String key) {
		return new ConfigEntryBuilder<D, String>(StringConfigEntry::new, key);
	}
}
