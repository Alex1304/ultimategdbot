package com.github.alex1304.ultimategdbot.modules.commands.impl.gdevents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleAwardedLevelsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleGDModeratorsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleTimelyLevelsSetting;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;
import sx.blah.discord.util.RequestBuffer;

/**
 * Allows users to subscribe to GD event notifications
 *
 * @author Alex1304
 */
public class GDEventsCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (!PermissionUtils.hasPermissions(event.getGuild(), UltimateGDBot.client().getOurUser(), Permissions.MANAGE_ROLES))
			throw new CommandFailedException("This command requires UltimateGDBot to be granted permission to manage roles in this server.");
		
		GuildSettings gs = DatabaseUtils.findByID(GuildSettings.class, event.getGuild().getLongID());
		
		if (gs == null) {
			gs = new GuildSettings();
			gs.setGuildID(event.getGuild().getLongID());
			DatabaseUtils.save(gs);
		}
		
		gs.setGuildInstance(event.getGuild());
		
		Map<String, IRole> roleMap = new HashMap<>();
		roleMap.put("awarded_levels", new RoleAwardedLevelsSetting(gs).getValue());
		roleMap.put("timely_levels", new RoleTimelyLevelsSetting(gs).getValue());
		roleMap.put("gd_moderators", new RoleGDModeratorsSetting(gs).getValue());
		
		Map<String, String> infoMap = new HashMap<>();
		infoMap.put("awarded_levels", "on new rated/featured levels!");
		infoMap.put("timely_levels", "on new Daily levels and Weekly demons!");
		infoMap.put("gd_moderators", "when RobTop promotes or demotes Geometry Dash moderators!");
		
		if (roleMap.values().stream().allMatch(x -> x == null))
			throw new CommandFailedException("There is no event you can subscribe to in this server.");
		
		InteractiveMenu im = new InteractiveMenu();
		im.setMenuContent("Type one of the following labels to toggle notifications for the associated event:");
		
		StringBuffer embedContent = new StringBuffer();
		
		for (String key : roleMap.keySet()) {
			IRole role = roleMap.get(key);
			String info = infoMap.get(key);
			if (role != null) {
				embedContent.append("`" + key + "` - Get notified " + info + "\n");
				
				im.addSubCommand(key, (event0, args0) -> {
					RequestBuffer.request(() -> {
						boolean hasRole = event0.getAuthor().hasRole(role);
						if (hasRole)
							event0.getAuthor().removeRole(role);
						else
							event0.getAuthor().addRole(role);
						
						BotUtils.sendMessage(event0.getChannel(), Emojis.SUCCESS + " Successfully " + 
								(hasRole ? "removed" : "added") + " the `" + role.getName() + "` role. You will " +
								(hasRole ? "no longer" : "now") + " receive notifications from this server " + info + "\n" +
								(hasRole ? "To get the role back" : "To remove the role") + ", run the command again.");
					});
				});
			}
		}
		
		im.setMenuEmbedContent(embedContent.toString());
		CommandsModule.executeCommand(im, event, args);
	}

}
