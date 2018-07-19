package com.github.alex1304.ultimategdbot.modules.commands.impl.checkmod;

import java.util.List;

import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.jdash.component.property.GDUserRole;
import com.github.alex1304.jdashevents.manager.GDEventManager;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GDMod;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.exceptions.UnknownUserException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows user to check for moderator state of a user. If the bot sees that the
 * state has changed, it will fire a user mod/unmod event.
 *
 * @author Alex1304
 */
public class CheckModCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		String keywords = BotUtils.concatCommandArgs(args);
		long accountID = -1;
		
		if (args.isEmpty()) {
			UserSettings us = DatabaseUtils.findByID(UserSettings.class, event.getAuthor().getLongID());
			
			if (us == null || !us.getLinkActivated())
				throw new InvalidCommandArgsException("If you want to check someone else's mod status, use `" + event.getMessage().getContent() + " <username or playerID>`, ex. `"
						+ event.getMessage().getContent() + " ViPriN` or `" + event.getMessage().getContent() + " 16`\n"
						+ "If you want to display your own mod status, you may want to link your Geomety Dash account first. To do so, use `"
						+ UltimateGDBot.property("ultimategdbot.prefix.canonical") + "account` and follow instructions. You will then be able to "
						+ "use the checkmod command without specifying a user.");
			
			accountID = us.getGdUserID();
		} else {
			accountID = GDUtils.guessGDUserIDFromString(keywords);
			if (accountID == -1)
				throw new UnknownUserException();
			if (accountID == -2)
				throw new GDServersUnavailableException();
		}
		
		final long finalAccountID = accountID;
		
		GDUser user = (GDUser) UltimateGDBot.cache()
				.readAndWriteIfNotExists("gd.user." + accountID, () -> 
					UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(finalAccountID)));
		
		if (user == null)
			throw new GDServersUnavailableException();
		
		BotUtils.sendMessage(event.getChannel(), "Checking mod access for user **" + user.getName() + "**...\n"
				+ (user.getRole() == GDUserRole.USER ? Emojis.FAILED + " Failed. Nothing found." :
						Emojis.SUCCESS + " Success! Access granted: " + user.getRole()));
		
		GDMod mod = DatabaseUtils.findByID(GDMod.class, user.getAccountID());
		
		if ((mod == null || !mod.getElder()) && user.getRole() == GDUserRole.ELDER_MODERATOR)
			GDEventManager.getInstance().dispatch("USER_PROMOTED_ELDER", user);
		else if (mod == null && user.getRole() == GDUserRole.MODERATOR)
			GDEventManager.getInstance().dispatch("USER_PROMOTED_MOD", user);
		else if (mod != null && mod.getElder() && user.getRole() == GDUserRole.MODERATOR)
			GDEventManager.getInstance().dispatch("USER_DEMOTED_MOD", user);
		else if (mod != null && user.getRole() == GDUserRole.USER)
			GDEventManager.getInstance().dispatch("USER_DEMOTED_USER", user);
		

		if (user.getRole() == GDUserRole.USER) {
			if (mod != null)
				DatabaseUtils.delete(mod);
		} else {
			if (mod == null)
				mod = new GDMod();
			
			mod.setAccountID(user.getAccountID());
			mod.setUsername(user.getName());
			mod.setElder(user.getRole() == GDUserRole.ELDER_MODERATOR);
			
			DatabaseUtils.save(mod);
		}
	}

}
