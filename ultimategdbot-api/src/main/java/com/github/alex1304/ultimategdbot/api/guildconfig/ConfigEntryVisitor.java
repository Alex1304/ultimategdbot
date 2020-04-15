package com.github.alex1304.ultimategdbot.api.guildconfig;

import reactor.core.publisher.Mono;

/**
 * Visitor that visits concrete types of {@link ConfigEntry}.
 * 
 * @param <G> Generic type G of {@link ConfigEntry}
 * @param <R> the return value type of the visitors
 */
public interface ConfigEntryVisitor<G extends GuildConfigData<G>, R> {
	
	Mono<R> visit(IntegerConfigEntry<G> entry);
	Mono<R> visit(LongConfigEntry<G> entry);
	Mono<R> visit(BooleanConfigEntry<G> entry);
	Mono<R> visit(StringConfigEntry<G> entry);
	Mono<R> visit(GuildChannelConfigEntry<G> entry);
	Mono<R> visit(GuildRoleConfigEntry<G> entry);
	Mono<R> visit(GuildMemberConfigEntry<G> entry);
}
