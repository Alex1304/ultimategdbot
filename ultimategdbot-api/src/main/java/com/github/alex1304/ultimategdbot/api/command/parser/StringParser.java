package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public class StringParser implements Parser<String> {

	@Override
	public Mono<String> parse(Context ctx, String input) {
		return Mono.just(input);
	}

	@Override
	public Class<String> type() {
		return String.class;
	}
}
