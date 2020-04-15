package com.github.alex1304.ultimategdbot.api.guildconfig;

import java.util.function.BiFunction;
import java.util.function.Function;

import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class GuildMemberConfigEntry<G extends GuildConfigData<G>> extends AbstractConfigEntry<G, Member> {

	GuildMemberConfigEntry(GuildConfigurator<G> configurator, String key, String description,
			Function<? super G, ? extends Mono<Member>> valueGetter,
			BiFunction<? super G, ? super Member, ? extends G> valueSetter, Validator<Member> validator) {
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
	public static <G extends GuildConfigData<G>> ConfigEntryBuilder<G, Member> builder(String key) {
		return new ConfigEntryBuilder<>(GuildMemberConfigEntry::new, key);
	}
}
