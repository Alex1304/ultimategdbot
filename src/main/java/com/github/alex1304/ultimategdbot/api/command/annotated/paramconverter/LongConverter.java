package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

public final class LongConverter implements ParamConverter<Long> {

	@Override
	public Mono<Long> convert(Context ctx, String input) {
		return Mono.just(input).map(Long::parseLong);
	}

	@Override
	public Class<Long> type() {
		return Long.class;
	}

}
