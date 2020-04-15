package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

public class GuildRoleConfigEntry<G extends GuildConfigData<G>> extends AbstractConfigEntry<G, Role> {

	GuildRoleConfigEntry(GuildConfigurator<G> configurator, String key, String description,
			Function<? super G, ? extends Mono<Role>> valueGetter,
			BiFunction<? super G, ? super Role, ? extends G> valueSetter, Validator<Role> validator) {
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
	public static <G extends GuildConfigData<G>> ConfigEntryBuilder<G, Role> builder(String key) {
		return new ConfigEntryBuilder<>(GuildRoleConfigEntry::new, key);
	}
}
