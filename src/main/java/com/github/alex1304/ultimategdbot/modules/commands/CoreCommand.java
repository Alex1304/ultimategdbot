package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.utils.BotRoles;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;


/**
 * Implementation of the Command interface that allows setting required roles to
 * use the command, and setting a map of subcommands.
 * 
 * @author Alex1304
 *
 */
public abstract class CoreCommand implements Command {
	
	protected String name;
	private EnumSet<BotRoles> rolesRequired;
	private Map<String, Command> subCommandMap;
	
	public CoreCommand(String name, EnumSet<BotRoles> rolesRequired) {
		this.name = name;
		this.rolesRequired = rolesRequired;
		this.subCommandMap = initSubCommandMap();
	}
	
	/**
	 * Initializes the private field subCommandMap.
	 * @return the initialized Map that will be stored into the private field.
	 */
	protected abstract Map<String, Command> initSubCommandMap();
	
	/**
	 * Gets a map containing all subcommands, mapped by their name
	 * 
	 * @return {@link Map}
	 */
	public Map<String, Command> getSubCommandMap() {
		return this.subCommandMap;
	}

	/**
	 * Triggers a subcommand by giving its name
	 * 
	 * @param cmdName - The name of the sub-command to trigger
	 * @param event - the Discord event info provided by the parent core command
	 * @param args - arguments to give to the subcommand
	 * @return false if the command could not be found, true otherwise
	 * @throws CommandFailedException
	 *             if the sub-command was unable to terminate correctly or if the
	 *             command syntax was invalid. Note that in most of the cases, the
	 *             parent command won't catch this exception and will propagate it
	 *             to the command handler.
	 */
	public boolean triggerSubCommand(String cmdName, MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (subCommandMap.containsKey(cmdName)) {
			subCommandMap.get(cmdName).runCommand(event, args);
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Setter for subCommandMap
	 * 
	 * @param subCommandMap
	 */
	public void setSubCommandMap(Map<String, Command> subCommandMap) {
		if (subCommandMap == null)
			this.subCommandMap = subCommandMap;
	}

	/**
	 * Get the name of the command
	 * 
	 * @return String name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the {@link EnumSet} of roles required to run this command
	 * 
	 * @return EnumSet<BotRoles> the roles required
	 */
	public EnumSet<BotRoles> getRolesRequired() {
		return rolesRequired;
	}
}
