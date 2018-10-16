package com.github.alex1304.ultimategdbot.logic;

/**
 * A procedure is a functional interface that takes no parameter and returns no
 * value. It has the same structure as functional interfaces in
 * java.util.function package.
 *
 * @author Alex1304
 */
@FunctionalInterface
public interface Procedure {
	
	/**
	 * Executes the procedure
	 */
	void run();

	/**
	 * Sets a procedure to be executed right after the current one
	 * 
	 * @param after - the other procedure
	 * @return Procedure
	 */
	default Procedure andThen(Procedure after) {
		return () -> {
			this.run();
			after.run();
		};
	}

	/**
	 * Sets a procedure to be executed right before the current one
	 * 
	 * @param before - the other procedure
	 * @return Procedure
	 */
	default Procedure compose(Procedure before) {
		return () -> {
			before.run();
			this.run();
		};
	}
	
	/**
	 * Converts the procedure into a runnable so it can be used in a thread
	 * 
	 * @return Runnable
	 */
	default Runnable toRunnable() {
		return () -> this.run();
	}
}
