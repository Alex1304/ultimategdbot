package com.github.alex1304.ultimategdbot.modules.commands.impl.setup;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.GuildSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.GuildSettingMapping;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * The setup command allows server admins to configure various things for the bot
 *
 * @author Alex1304
 */
public class SetupCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		GuildSettings gs = DatabaseUtils.findByID(GuildSettings.class, event.getGuild().getLongID());
		
		if (gs == null)
			gs = new GuildSettings();
		
		gs.setGuildInstance(event.getGuild());
		
		Iterator<GuildSettingMapping<?, ?, ?>> settings = GuildSetting.iterateSettings();
		StringBuffer settingsList = new StringBuffer();
		
		while (settings.hasNext()) {
			GuildSettingMapping<?, ?, ?> gsm = settings.next();
			settingsList.append("**" + gsm.getName() + "**: " + gsm.getInstanceFunc().apply(gs) + "\n");
			
		}
		
		final GuildSettings finalGs = gs;
		InteractiveMenu setupMenu = new InteractiveMenu();
		Procedure reopenMenu = () -> CommandsModule.executeCommand(this, event, args);
		
		setupMenu.setMenuContent("This menu will allow you to configure UltimateGDBot for your server."
				+ "You can for example choose a channel for Geometry Dash event notifications and choose "
				+ "a role to tag. More configurable things might be added in the future so check this commmand"
				+ "from time to time to see if the configuration fits with your server's needs!\n\n"
				+ settingsList.toString());
		
		setupMenu.setMenuEmbedContent("To assign a value to a field, type `set <field_name> <new_value>`, ex. "
						+ "`set channel_awarded_levels #yourchannel`\n"
						+ "To empty/reset a field to default value, type `reset <field_name>`");
		
		setupMenu.addSubCommand("set", (event0, args0) -> {
			if (args0.size() < 2) {
				reopenMenu.run();
				throw new InvalidCommandArgsException("`set <field_name> <new_value>`, ex. "
						+ "`set channel_awarded_levels #yourchannel`");
			}
			
			String name = args0.get(0);
			String value = BotUtils.concatCommandArgs(args0.subList(1, args0.size()));
			
			GuildSetting<?, ?> setting = GuildSetting.get(name, finalGs);
			
			if (setting == null)
				throw new CommandFailedException("The field " + name + " does not exist.");
			try {
				setting.save(value);
				reopenMenu.run();
				BotUtils.sendMessage(event0.getChannel(), Emojis.SUCCESS + " Settings updated");
			} catch (IllegalArgumentException e) {
				reopenMenu.run();
				throw new CommandFailedException("Unexpected value");
			}
		});
		
		setupMenu.addSubCommand("reset", (event0, args0) -> {
			if (args0.isEmpty()) {
				reopenMenu.run();
				throw new InvalidCommandArgsException("`reset <field_name>`, ex. "
						+ "`reset channel_awarded_levels`");
			}
			
			String name = args0.get(0);
			
			GuildSetting<?, ?> setting = GuildSetting.get(name, finalGs);
			
			if (setting == null)
				throw new CommandFailedException("The field " + name + " does not exist.");
			
			setting.save("");
			reopenMenu.run();
			BotUtils.sendMessage(event0.getChannel(), Emojis.SUCCESS + " Field has been reset");
		});
		
		CommandsModule.executeCommand(setupMenu, event, args);
	}
	
	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.SERVER_ADMIN);
	}
}
