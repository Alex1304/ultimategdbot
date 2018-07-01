package com.github.alex1304.ultimategdbot.modules.commands.impl.help;

import com.github.alex1304.ultimategdbot.modules.commands.SuperCommand;
import com.github.alex1304.ultimategdbot.modules.commands.impl.help.list.HelpListCommand;

/**
 * Help command gives details on the command list, tells the user how to use the
 * bot, etc.
 *
 * @author Alex1304
 */
public class HelpCommand extends SuperCommand {
	
	public HelpCommand() {
		this.addSubCommand("list", new HelpListCommand());
		
		this.addMenuItem("To see the list of available commands, type `list`");
	}

}
