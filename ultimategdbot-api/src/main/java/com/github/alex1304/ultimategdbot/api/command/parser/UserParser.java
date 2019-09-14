package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.ArgumentParseException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.DiscordParser;

import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

public class UserParser implements Parser<User> {

	@Override
	public Mono<User> parse(Context ctx, String input) {
		return DiscordParser.parseUser(ctx.getBot(), input)
				.onErrorMap(e -> new ArgumentParseException(e.getMessage()));
	}

	@Override
	public Class<User> type() {
		return User.class;
	}
}
