package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;

public class GuildChannelConfigEntry extends AbstractConfigEntry<GuildChannel> {


	GuildChannelConfigEntry(GuildConfigurator<?> configurator, String key, String description,
			Function<Object, ? extends Mono<GuildChannel>> valueGetter,
			BiFunction<Object, ? super GuildChannel, Object> valueSetter, Validator<GuildChannel> validator,
			Consumer<? super GuildChannel> valueObserver) {
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
	public static <D extends GuildConfigData<D>> ConfigEntryBuilder<D, GuildChannel> builder(String key) {
		return new ConfigEntryBuilder<>(GuildChannelConfigEntry::new, key);
	}
}
