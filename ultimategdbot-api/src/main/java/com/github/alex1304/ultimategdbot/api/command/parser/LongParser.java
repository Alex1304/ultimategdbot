package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public class LongParser implements Parser<Long> {

	@Override
	public Mono<Long> parse(Context ctx, String input) {
		return Mono.just(input).map(Long::parseLong);
	}

	@Override
	public Class<Long> type() {
		return Long.class;
	}

}
