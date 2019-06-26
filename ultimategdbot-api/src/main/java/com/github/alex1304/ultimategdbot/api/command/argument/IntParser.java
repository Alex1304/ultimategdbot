package com.github.alex1304.ultimategdbot.api.command.argument;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public class IntParser implements ArgumentParser<Integer> {

	@Override
	public Mono<Integer> parse(Context ctx, String input) {
		return Mono.just(input).map(Integer::parseInt);
	}

	@Override
	public Class<Integer> type() {
		return Integer.class;
	}

}
