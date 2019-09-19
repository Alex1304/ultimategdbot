package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public class IntConverter implements ParamConverter<Integer> {

	@Override
	public Mono<Integer> convert(Context ctx, String input) {
		return Mono.just(input).map(Integer::parseInt);
	}

	@Override
	public Class<Integer> type() {
		return Integer.class;
	}

}
