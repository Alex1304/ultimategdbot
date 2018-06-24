package com.github.alex1304.ultimategdbot.modules.commands;

/**
 * A sub-command is a command that is triggered by other commands, not by an event.
 * It contains info about their parent commands
 * 
 * @author Alex1304
 *
 */
public abstract class SubCommand<T extends Command> implements Command {
	
	private T parentCommand;
	
	public SubCommand(T parentCommand) {
		super();
		this.parentCommand = parentCommand;
	}
	
	/**
	 * Gets the direct parent {@link Command}. It can be either a {@link CoreCommand} or another {@link SubCommand}
	 * @return an instance of {@link Command}
	 */
	public T getParentCommand() {
		return parentCommand;
	}
}
