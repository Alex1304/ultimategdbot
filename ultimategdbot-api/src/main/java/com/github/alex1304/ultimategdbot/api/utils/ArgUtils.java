package com.github.alex1304.ultimategdbot.api.utils;

import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;

/**
 * Contains handy methods to deal with command arguments.
 */
public class ArgUtils {
	
	/**
	 * Checks if the arguments of htis context has an argument count greater or
	 * equal to the given count. If the condition isn't satisfied, an
	 * {@link InvalidSyntaxException} is thrown.
	 * 
	 * @param ctx   the context
	 * @param count the target minimum arg count
	 * @throws InvalidSyntaxException if the args of the context don't
	 *         satify the given count
	 */
	public static void requireMinimumArgCount(Context ctx, int count) {
		if (ctx.getArgs().size() < count) {
			throw new InvalidSyntaxException(ctx.getCommand());
		}
	}
	
	/**
	 * Checks if the arguments of htis context has an argument count greater or
	 * equal to the given count. If the condition isn't satisfied, an
	 * {@link CommandFailedException} is thrown with a personalized message.
	 * 
	 * @param ctx   the context
	 * @param count the target minimum arg count
	 * @param errorMessage the error message
	 * @throws CommandFailedException if the args of the context dsn't
	 *         satify the given count
	 */
	public static void requireMinimumArgCount(Context ctx, int count, String errorMessage) {
		if (ctx.getArgs().size() < count) {
			throw new CommandFailedException(errorMessage);
		}
	}
	
	/**
	 * Concatenates all arguments from the specified index to the end.
	 * 
	 * @param ctx       the context
	 * @param fromIndex index from which to start concatenate
	 * @return the concatenated args
	 * @throws IndexOutOfBoundsException if fromIndex is lower than 0 or is greater
	 *                                   or equal to the arg count
	 */
	public static String concatArgs(Context ctx, int fromIndex) {
		return String.join(" ", ctx.getArgs().subList(fromIndex, ctx.getArgs().size()));
	}
	
	/**
	 * Gets the value of the argument at the specified index as int.
	 * 
	 * @param ctx   the context
	 * @param index the argument index
	 * @return the value of the argument as int
	 * @throws IndexOutOfBoundsException if index is out of bounds
	 * @throws CommandFailedException    if the value could not be parsed as int
	 */
	public static int getArgAsInt(Context ctx, int index) {
		try {
			return Integer.parseInt(ctx.getArgs().get(index));
		} catch (NumberFormatException e) {
			throw new CommandFailedException("Argument " + index + " is invalid: numeric value expected.");
		}
	}
	
	/**
	 * Gets the value of the argument at the specified index as long.
	 * 
	 * @param ctx   the context
	 * @param index the argument index
	 * @return the value of the argument as long
	 * @throws IndexOutOfBoundsException if index is out of bounds
	 * @throws CommandFailedException    if the value could not be parsed as long
	 */
	public static long getArgAsLong(Context ctx, int index) {
		try {
			return Long.parseLong(ctx.getArgs().get(index));
		} catch (NumberFormatException e) {
			throw new CommandFailedException("Argument " + index + " is invalid: numeric value expected.");
		}
	}
}
