package com.github.alex1304.ultimategdbot.api.command.argument;

import com.github.alex1304.ultimategdbot.api.command.ArgumentParseException;
import com.github.alex1304.ultimategdbot.api.command.Context;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class UserParser implements ArgumentParser<User> {

	@Override
	public Mono<User> parse(Context ctx, String input) {
		String id;
		if (input.matches("[0-9]{1,19}")) {
			id = input;
		} else if (input.matches("<@!?[0-9]{1,19}>")) {
			id = input.substring(input.startsWith("<@!") ? 3 : 2, input.length() - 1);
		} else {
			return Mono.error(new ArgumentParseException("Not a valid mention/ID."));
		}
		return Mono.just(id)
				.map(Snowflake::of)
				.onErrorMap(e -> new ArgumentParseException("Not a valid mention/ID."))
				.flatMap(snowflake -> ctx.getBot().getMainDiscordClient().getUserById(snowflake))
				.onErrorMap(e -> new ArgumentParseException("Could not resolve the mention/ID to a valid user."));
	}

	@Override
	public Class<User> type() {
		return User.class;
	}
}
