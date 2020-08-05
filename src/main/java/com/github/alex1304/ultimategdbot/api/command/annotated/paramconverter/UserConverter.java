package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.util.DiscordParser;

import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

public final class UserConverter implements ParamConverter<User> {

	@Override
	public Mono<User> convert(Context ctx, String input) {
		return DiscordParser.parseUser(ctx, ctx.event().getClient(), input);
	}

	@Override
	public Class<User> type() {
		return User.class;
	}
}
