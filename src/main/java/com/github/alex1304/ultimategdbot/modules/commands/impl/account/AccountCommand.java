package com.github.alex1304.ultimategdbot.modules.commands.impl.account;

import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.jdash.api.request.GDMessageListHttpRequest;
import com.github.alex1304.jdash.api.request.GDMessageReadHttpRequest;
import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.api.request.GDUserSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDMessage;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.jdash.component.GDUserPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.DatabaseFailureException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.modules.reply.Reply;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

/**
 * Allows users to link their Discord account with their Geometry Dash account.
 * They need to prove they own the account before linking.
 * They are free to unlink their account at anytime.
 *
 * @author Alex1304
 */
public class AccountCommand implements Command {
	
	public static final int TOKEN_LENGTH = 6;

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (!UltimateGDBot.isModuleAvailable("reply"))
			throw new CommandFailedException("This command is temporarily unavailable. Try again later.");
		
		UserSettings us = DatabaseUtils.findByID(UserSettings.class, event.getAuthor().getLongID());
		InteractiveMenu menu = new InteractiveMenu();
		StringBuffer menuContent = new StringBuffer(
				"You can link your Discord account with your Geometry Dash account to get access to "
						+ "cool stuff in UltimateGDBot. You can for example use the `profile` command without arguments to display your own info, "
						+ "let others easily access your profile by mentionning you, or appear in server-wide Geometry Dash "
						+ "leaderboards.\n\n");
		StringBuffer menuEmbedContent = new StringBuffer();
		Procedure rerunCmd = () -> CommandsModule.executeCommand(this, event, new ArrayList<>());

		if (us == null || (!us.getLinkActivated() && us.getConfirmationToken() == null)) {
			menuContent.append("You are currently not linked to any account!");
			
			menuEmbedContent.append("To start linking your account, type `link` followed by your Geometry Dash name "
					+ "or playerID, for example if your GD name is \"ExamplePlayer01\" type `link ExamplePlayer01`");
			
			
			menu.addSubCommand("link", (event0, args0) -> {
				if (args0.isEmpty()) {
					rerunCmd.run();
					throw new InvalidCommandArgsException("`link <GD name or playerID>`, ex. `link ExamplePlayer01`");
				}
				
				String username = BotUtils.concatCommandArgs(args0);
				
				GDComponentList<GDUserPreview> results = (GDComponentList<GDUserPreview>) UltimateGDBot.cache()
						.readAndWriteIfNotExists("gd.usersearch." + username, () -> 
								UltimateGDBot.gdClient().fetch(new GDUserSearchHttpRequest(username, 0)));
				
				if (results == null || results.isEmpty()) {
					rerunCmd.run();
					throw new CommandFailedException("Unable to fetch user `" + username + "`");
				}
				
				GDUserPreview gup = results.get(0);
				String token = BotUtils.generateAlphanumericToken(TOKEN_LENGTH);
				boolean saved = DatabaseUtils.save(new UserSettings(event0.getAuthor().getLongID(), gup.getAccountID(), false, token));
				if (!saved)
					throw new DatabaseFailureException();
				
				rerunCmd.run();
			});
		} else  {
			GDUser user = (GDUser) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.user." + us.getGdUserID(), () -> 
							UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(us.getGdUserID())));
			
			if (user == null) {
				menuContent.append(":warning: You seem to be linked to a Geometry Dash account, but I can't find "
						+ "the associated user in Geometry Dash. Try to run this command again later, or you may want to "
						+ "unlink your account.");
			} else {
				if (!us.getLinkActivated()) {
					menuContent.append("You have requested to link your Discord account with the Geometry Dash "
							+ "account **" + user.getName() + "**. Now you need to prove that you are the owner of "
							+ "this account. Please follow the instructions below to finalize the linking "
							+ "process.\n");
					
					menuEmbedContent.append("Step 1: Open Geometry Dash\n");
					menuEmbedContent.append("Step 2: Search for user \"UltimateGDBot\" and open profile\n");
					menuEmbedContent.append("Step 3: Click the button to send a private message\n");
					menuEmbedContent.append("Step 4: In the \"Subject\" field, input `Confirm` (case insensitive)\n");
					menuEmbedContent.append("Step 5: In the \"Body\" field, input the code `" + us.getConfirmationToken() + "` (:warning: case sensitive)\n");
					menuEmbedContent.append("Step 6: Send the message, then go back to Discord in this channel and type `done`. If the command has timed out, just "
							+ "re-run the account command and type `done`\n");
					menuEmbedContent.append("───────────────────\n");
					
					menu.addSubCommand("done", (event0, args0) -> {
						try {
							GDComponentList<GDMessage> inbox = UltimateGDBot.gdClient().fetch(new GDMessageListHttpRequest(0));
							boolean isVerified = inbox.stream()
									.anyMatch(x -> {
										if (x.getSenderID() == us.getGdUserID() && x.getSubject().equalsIgnoreCase("confirm")) {
											try {
												GDMessage m = UltimateGDBot.gdClient().fetch(new GDMessageReadHttpRequest(x.getMessageID()));
												return m.getBody().equals(us.getConfirmationToken());
											} catch (GDAPIException e) {
												return false;
											}
										}
										return false;
									});
							
							if (isVerified) {
								us.setLinkActivated(true);
								us.setConfirmationToken(null);
								
								if (DatabaseUtils.save(us))
								BotUtils.sendMessage(event0.getChannel(), Emojis.SUCCESS + " You are now linked to Geometry Dash account **" + user.getName() + "**!");
							} else {
								rerunCmd.run();
								throw new CommandFailedException("Unable to verify your account. Make sure you have followed the steps given to verify your account and try again.");
							}
						} catch (GDAPIException e) {
							throw new CommandFailedException("Geometry Dash servers are unavailable so I am unable to verify your account. Try again later.");
						}
					});
				} else {
					menuContent.append("You are currently linked to the Geometry Dash "
						+ "account **" + user.getName() + "**!");
				}
			}
			
			menuEmbedContent.append("To unlink your account or cancel the linking process, type `unlink`");
			
			menu.addSubCommand("unlink", (event0, args0) -> {
				Procedure doUnlink = () -> {
					if (DatabaseUtils.delete(us))
						BotUtils.sendMessage(event0.getChannel(), Emojis.SUCCESS + " Successfully unlinked your account");
				};
				
				try {
					ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
					IMessage confirm = BotUtils.sendMessage(event0.getChannel(), ":warning: Are you sure you want to unlink your account? "
							+ "Reply with `confirm` or `cancel`.");
					Reply r = new Reply(confirm, event0.getAuthor(), message -> message.getContent().equalsIgnoreCase("confirm"));
					r.setOnSuccess(() -> doUnlink.run()); 
					rm.open(r, true, true);
				} catch (ModuleUnavailableException e) {
					doUnlink.run();
				}
			});
		}
		
		menu.setMenuContent(menuContent.toString());
		menu.setMenuEmbedContent(menuEmbedContent.toString());
		
		CommandsModule.executeCommand(menu, event, args);
	}

}
