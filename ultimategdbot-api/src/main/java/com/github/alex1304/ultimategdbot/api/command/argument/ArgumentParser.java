package com.github.alex1304.ultimategdbot.api.command.argument;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

/**
 * Parses a command argument.
 * 
 * @param <T> the type of object this argument should be parsed to
 */
public interface ArgumentParser<T> {
	/**
	 * Accepts a String input and parses it into an object of type T. This method
	 * has asynchronous capabilities, and may error to indicate that it cannot be
	 * parsed to the desired object. An empty mono may represent the fact that an
	 * argument is optional, ignored or non applicable.
	 * 
	 * @param input the input to parse
	 * @return a Mono emittng the result of the parsing, a error if the parsing
	 *         fails, or empty if non applicable or not provided.
	 */
	Mono<T> parse(Context ctx, String input);
	
	/**
	 * Returns the type of object this parser can parse arguments to
	 * 
	 * @return a Class
	 */
	Class<T> type();
}
