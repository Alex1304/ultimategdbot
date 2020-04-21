package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

/**
 * Parses a String into an object of parameterized type. It takes into account
 * the context of a command to allow performing case by case parsing.
 * 
 * @param <T> the type of object this argument should be parsed to
 */
public interface ParamConverter<T> {
	/**
	 * Accepts a String input and parses it into an object of type T. This method
	 * has asynchronous capabilities, and may error to indicate that it cannot be
	 * parsed to the desired object. An empty mono may represent the fact that an
	 * argument is optional, ignored or non applicable.
	 * 
	 * @param ctx the context of a command that may influence the parsing process
	 * @param input the input to parse
	 * @return a Mono emittng the result of the parsing, a error if the parsing
	 *         fails, or empty if non applicable or not provided.
	 */
	Mono<T> convert(Context ctx, String input);
	
	/**
	 * Returns the type of object resulting of the parsing.
	 * 
	 * @return a Class
	 */
	Class<T> type();
}
