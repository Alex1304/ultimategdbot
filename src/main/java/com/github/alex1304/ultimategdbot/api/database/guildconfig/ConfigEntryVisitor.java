package com.github.alex1304.ultimategdbot.api.database.guildconfig;

import reactor.core.publisher.Mono;

/**
 * Visitor that visits concrete types of {@link ConfigEntry}.
 * 
 * @param <R> the return value type of the visitors
 */
public interface ConfigEntryVisitor<R> {
	
	Mono<R> visit(IntegerConfigEntry entry);
	Mono<R> visit(LongConfigEntry entry);
	Mono<R> visit(BooleanConfigEntry entry);
	Mono<R> visit(StringConfigEntry entry);
	Mono<R> visit(GuildChannelConfigEntry entry);
	Mono<R> visit(GuildRoleConfigEntry entry);
	Mono<R> visit(GuildMemberConfigEntry entry);
}
