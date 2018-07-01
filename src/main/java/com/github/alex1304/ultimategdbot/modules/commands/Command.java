package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.EnumSet;
import java.util.List;

import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.utils.BotRoles;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * A command is a sub-program that is triggered by another program or a person.
 * The behavior of a command can be influenced by the arguments given.
 * This is a core feature for the bot, as it is the only way for it to communicate 
 * and interact with Discord users.
 * 
 * @author Alex1304
 *
 */
public interface Command {

	/**
	 * Executes the command
	 * 
	 * @param event
	 *            - contains all info about the user who launched the command,
	 *            the channels, the guild, etc
	 * @param args
	 *            - arguments the user provided with the command
	 * @throws CommandFailedException
	 *             if the command was unable to terminate correctly or if the
	 *             command syntax is invalid.
	 */
	void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException;
	
	/**
	 * Allows to set role restrictions on the command. By default, everyone can use the command.
	 * 
	 * @return EnumSet&lt;BotRoles&gt;
	 */
	default EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.USER);
	}
	
	/**
	 * Returns the parent command. This is optional and returns null by default.
	 * 
	 * @return Command
	 */
	default Command getParentCommand() {
		return null;
	}
}
