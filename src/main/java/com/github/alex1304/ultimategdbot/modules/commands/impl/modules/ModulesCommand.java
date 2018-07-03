package com.github.alex1304.ultimategdbot.modules.commands.impl.modules;

import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

/**
 * Admin command to manage modules
 *
 * @author Alex1304
 */
public class ModulesCommand extends InteractiveMenu {

	public ModulesCommand() {
		this.setMenuContent("To view the full list of modules and their status, type `list`\n"
				+ "To start a module, type `start` followed by the module name, ex. `start commands`\n"
				+ "To stop a module, type `stop` followed by the module name, ex. `stop commands`\n"
				+ "To restart a module, type `restart` followed by the module name, ex. `restart commands`");

		this.addSubCommand("list", (event, args) -> {
			Map<String, Boolean> modules = UltimateGDBot.getStartedModules();

			StringBuffer sb = new StringBuffer();
			sb.append("List of modules:\n");
			sb.append("```");
			sb.append(BotUtils.truncate("name", 16) + " | " + "status");
			sb.append("\n---------------------------\n");
			for (Entry<String, Boolean> m : modules.entrySet()) {
				sb.append(BotUtils.truncate(m.getKey(), 16));
				sb.append(" | ");
				sb.append(m.getValue() ? "OK" : "STOPPED");
				sb.append('\n');
			}
			sb.append("```");
			BotUtils.sendMessage(event.getChannel(), sb.toString());
		});
		
		this.addSubCommand("start", (event, args) -> {
			if (args.isEmpty())
				throw new InvalidCommandArgsException("Please prodide a module name");
			UltimateGDBot.startModule(args.get(0));
		});

		this.addSubCommand("stop", (event, args) -> {
			if (args.isEmpty())
				throw new InvalidCommandArgsException("Please prodide a module name");
			UltimateGDBot.stopModule(args.get(0));
		});

		this.addSubCommand("restart", (event, args) -> {
			if (args.isEmpty())
				throw new InvalidCommandArgsException("Please prodide a module name");
			UltimateGDBot.restartModule(args.get(0));
		});
	}

	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.OWNER);
	}

}
