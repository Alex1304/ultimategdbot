package com.github.alex1304.ultimategdbot.modules.commands.impl.modlist;

import java.util.List;

import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GDMod;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows users to see the list of GD moderators
 *
 * @author Alex1304
 */
public class ModListCommand implements Command {

	
	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		List<GDMod> mods = DatabaseUtils.query(GDMod.class, "from GDMod");
		
		StringBuffer sb = new StringBuffer(event.getAuthor().mention() + ", here is the latest known list of Geometry Dash moderators:\n\n");
		
		mods.stream()
			.sorted((x, y) -> x.getUsername().isEmpty() || y.getUsername().isEmpty() ? (int) (x.getAccountID() - y.getAccountID()) : x.getUsername().compareTo(y.getUsername()))
			.forEachOrdered(m -> {
				if (m.getUsername().isEmpty()) {
					GDUser user = (GDUser) UltimateGDBot.cache()
							.readAndWriteIfNotExists("gd.user." + m.getAccountID(), () ->
									UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(m.getAccountID())));
					if (user != null) {
						m.setUsername(user.getName());
						DatabaseUtils.save(m);
					}
				}
				sb.append((m.getElder() ? Emojis.ELDER_MOD : Emojis.MOD) + " " + (m.getUsername().isEmpty() ? "Unknown user (" + m.getAccountID() + ")" : m.getUsername()) + "\n");
			});
		
		BotUtils.sendMessage(event.getChannel(), sb.toString());
	}
}
