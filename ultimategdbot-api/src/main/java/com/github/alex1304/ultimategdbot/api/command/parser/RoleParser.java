package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.ArgumentParseException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.DiscordParser;

import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

public class RoleParser implements Parser<Role> {

	@Override
	public Mono<Role> parse(Context ctx, String input) {
		return Mono.justOrEmpty(ctx.getEvent().getGuildId())
				.flatMap(guildId -> DiscordParser.parseRole(ctx.getBot(), guildId, input))
				.onErrorMap(e -> new ArgumentParseException(e.getMessage()))
				.switchIfEmpty(Mono.error(new ArgumentParseException("Cannot find roles outside of a guild.")));
	}

	@Override
	public Class<Role> type() {
		return Role.class;
	}
}
