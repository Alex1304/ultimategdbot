package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public class IntParser implements Parser<Integer> {

	@Override
	public Mono<Integer> parse(Context ctx, String input) {
		return Mono.just(input).map(Integer::parseInt);
	}

	@Override
	public Class<Integer> type() {
		return Integer.class;
	}

}
