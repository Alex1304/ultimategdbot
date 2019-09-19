package com.github.alex1304.ultimategdbot.api.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.ParamConverter;

import reactor.core.publisher.Mono;

public class ArgumentList {

	private final List<String> tokens;
	private final Context context;
	
	public ArgumentList(List<String> tokens, Context context) {
		this.tokens = tokens;
		this.context = context;
	}

	public int tokenCount() {
		return tokens.size();
	}
	
	public String get(int position) {
		return tokens.get(position);
	}
	
	public String getAllAfter(int position) {
		return new ArrayDeque<>(getTokens(position + 1)).getLast();
	}
	
	public <T> Mono<T> parseAndGet(int position, ParamConverter<T> parser) {
		return Mono.fromCallable(() -> get(position)).flatMap(arg -> parser.convert(context, arg));
	}

	public List<String> getTokens() {
		return Collections.unmodifiableList(tokens);
	}
	
	public List<String> getTokens(int maxCount) {
		var mergedTokens = new ArrayDeque<>(tokens);
		while (mergedTokens.size() > 1 && mergedTokens.size() > maxCount) {
			var lastArg = mergedTokens.removeLast();
			var beforeLastArg = mergedTokens.removeLast();
			mergedTokens.addLast(beforeLastArg + " " + lastArg);
		}
		return Collections.unmodifiableList(new ArrayList<>(mergedTokens));
	}
}
