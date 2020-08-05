package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public final class StringConverter implements ParamConverter<String> {

	@Override
	public Mono<String> convert(Context ctx, String input) {
		return Mono.just(input);
	}

	@Override
	public Class<String> type() {
		return String.class;
	}
}
