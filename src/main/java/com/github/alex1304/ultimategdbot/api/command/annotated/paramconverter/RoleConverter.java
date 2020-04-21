package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.util.DiscordParser;

import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

public class RoleConverter implements ParamConverter<Role> {

	@Override
	public Mono<Role> convert(Context ctx, String input) {
		return Mono.justOrEmpty(ctx.event().getGuildId())
				.flatMap(guildId -> DiscordParser.parseRole(ctx.bot(), guildId, input))
				.switchIfEmpty(Mono.error(new RuntimeException("Cannot find roles outside of a guild.")));
	}

	@Override
	public Class<Role> type() {
		return Role.class;
	}
}
