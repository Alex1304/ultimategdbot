package com.github.alex1304.ultimategdbot.modules.commands.impl.profile;

import java.util.List;
import java.util.NoSuchElementException;

import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.api.request.GDUserSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.jdash.component.GDUserPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Command to display a user's profile
 *
 * @author Alex1304
 */
public class ProfileCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		try {
			String keywords = BotUtils.concatCommandArgs(args);
			long accountID = -1;
			
			if (args.isEmpty()) {
				UserSettings us = DatabaseUtils.findByID(UserSettings.class, event.getAuthor().getLongID());
				
				if (us == null || !us.getLinkActivated())
					throw new InvalidCommandArgsException("If you want to show someone else's profile, use `" + event.getMessage().getContent() + " <username or playerID>`, ex. `"
							+ event.getMessage().getContent() + " ViPriN` or `" + event.getMessage().getContent() + " 16`\n"
							+ "If you want to display your own profile, you may want to link your Geomety Dash account first. To do so, use `"
							+ UltimateGDBot.property("ultimategdbot.prefix.canonical") + "account` and follow instructions. You will then be able to "
							+ "use the profile command without specifying a user.");
				
				accountID = us.getGdUserID();
			} else {
				try {
					long userID = BotUtils.extractIDFromMention(keywords);
					UserSettings us = DatabaseUtils.findByID(UserSettings.class, userID);
					
					if (us == null || !us.getLinkActivated())
						throw new NoSuchElementException();
					
					accountID = us.getGdUserID();
				} catch (IllegalArgumentException | NoSuchElementException e) {
					GDComponentList<GDUserPreview> results = (GDComponentList<GDUserPreview>) UltimateGDBot.cache()
							.readAndWriteIfNotExists("gd.usersearch." + keywords, () -> {
								BotUtils.typing(event.getChannel(), true);
								return UltimateGDBot.gdClient().fetch(new GDUserSearchHttpRequest(keywords, 0));
							});
					
					if (results.isEmpty())
						throw new CommandFailedException("User not found.");
					
					accountID = results.get(0).getAccountID();
				}
			}
			
			final long finalAccountID = accountID;
			
			GDUser user = (GDUser) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.user." + accountID, () -> 
						UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(finalAccountID)));
			
			BotUtils.sendMessage(event.getChannel(), event.getAuthor().mention() + ", here is the profile of "
					+ "user **" + user.getName() + "** :", GDUtils.buildEmbedForGDUser("User profile",
							"https://i.imgur.com/ppg4HqJ.png", user));
			
		} catch (GDAPIException e) {
			throw new GDServersUnavailableException();
		} catch (CommandFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			BotUtils.typing(event.getChannel(), false);
		}
	}

}
