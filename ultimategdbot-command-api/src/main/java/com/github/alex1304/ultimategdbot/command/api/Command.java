package com.github.alex1304.ultimategdbot.command.api;

/**
 * Interface that bot commands should implement.
 * A command takes a context in parameter and returns a view. 
 *
 * @author Alex1304
 *
 * @param <C> - The type of the context object
 * @param <V> - The type of the view object
 */
public interface Command<C, V> {
	
	/**
	 * Executes the command
	 * 
	 * @param ctx - the context
	 * @return a View
	 * @throws CommandFailedException if something goes wrong when executing the command
	 */
	V execute(C ctx) throws CommandFailedException;
}
