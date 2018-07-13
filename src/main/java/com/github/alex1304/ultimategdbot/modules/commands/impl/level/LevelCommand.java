package com.github.alex1304.ultimategdbot.modules.commands.impl.level;

import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.jdash.api.request.GDLevelHttpRequest;
import com.github.alex1304.jdash.api.request.GDLevelSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevel;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.NavigationMenu;
import com.github.alex1304.ultimategdbot.modules.reply.Reply;
import com.github.alex1304.ultimategdbot.modules.reply.ReplyModule;
import com.github.alex1304.ultimategdbot.utils.AuthorObjects;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

/**
 * Allows user to search for a level in Geometry Dash
 *
 * @author Alex1304
 */
public class LevelCommand implements Command {
	
	private int page;
	
	public LevelCommand() {
		this.page = 0;
	}
	
	public LevelCommand(int page) {
		this.page = page;
	}

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (args.isEmpty())
			throw new InvalidCommandArgsException("`" + event.getMessage().getContent() + " <name or ID>`, ex `"
					+ event.getMessage().getContent() + " Level Easy`");

		final Procedure rollBackResults = () -> CommandsModule.executeCommand(this, event, args);
		
		String keywords = BotUtils.concatCommandArgs(args);
		String cacheID = "gd.levelsearch." + keywords + page;
			
		
		GDComponentList<GDLevelPreview> results = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
				.readAndWriteIfNotExists(cacheID, () -> {
					BotUtils.typing(event.getChannel(), true);
					return UltimateGDBot.gdClient().fetch(new GDLevelSearchHttpRequest(keywords, page));
				});
		
		if (results == null)
			throw new GDServersUnavailableException();
		
		if (results.isEmpty() && page == 0)
			throw new CommandFailedException("No results found.");
		
		if (results.size() == 1) {
			this.openLevel(results.get(0), event, false, rollBackResults);
			return;
		}
		
		if (!UltimateGDBot.isModuleAvailable("reply")) {
			BotUtils.sendMessage(event.getChannel(), buildResultOutput(results, page) +
					"\nAbility to navigate through search results is currently unavailable. Sorry for the inconvenience.");
			return;
		}
		
		NavigationMenu nm = new NavigationMenu(page, 9999, page -> new LevelCommand(page), event, args);
		
		nm.setMenuContent(buildResultOutput(results, page));
		nm.setMenuEmbedContent("To view full info on a level, type `select` followed by the search "
				+ "result number, ex. `select 2`\n");
		
		nm.addSubCommand("select", (event0, args0) -> {
			if (args0.isEmpty()) {
				rollBackResults.run();
				throw new InvalidCommandArgsException("`select <number>`, ex. `select 2`");
			}
			
			int selectInput = 1;
			
			try {
				selectInput = Integer.parseInt(args0.get(0));
				if (selectInput < 1 || selectInput > results.size()) {
					rollBackResults.run();
					throw new CommandFailedException("Result number out of range");
				}
			} catch (NumberFormatException e) {
				rollBackResults.run();
				throw new CommandFailedException("Sorry, `" + args0.get(0) + "` isn't a valid page number");
			}
			this.openLevel(results.get(selectInput - 1), event, true, rollBackResults);
		});
		
		CommandsModule.executeCommand(nm, event, new ArrayList<>());
		BotUtils.typing(event.getChannel(), false);
	}

	private String buildResultOutput(GDComponentList<GDLevelPreview> results, int page) {
		StringBuffer output = new StringBuffer();
		output.append("Page: ");
		output.append(page + 1);
		output.append("\n\n");
		
		int i = 1;
		for (GDLevelPreview lp : results) {
			String coins = GDUtils.coinsToEmoji(lp.getCoinCount(), lp.hasCoinsVerified(), true);
			output.append(String.format("`%02d` - %s%s | __**%s**__ by **%s** (%d) %s%s\n"
					+ "      Song: %s\n",
					i,
					GDUtils.difficultyToEmoji(lp),
					coins.equals("None") ? "" : " " + coins,
					lp.getName(),
					lp.getCreatorName(),
					lp.getId(),
					lp.getOriginalLevelID() > 0 ? Emojis.COPY : "",
					lp.getObjectCount() > 40000 ? Emojis.OBJECT_OVERFLOW : "",
					GDUtils.formatSongPrimaryMetadata(lp.getSong())));
			i++;
		}
		
		if (results.isEmpty())
			output.append("No results found.");
		
		return output.toString();
	}
	
	private void openLevel(GDLevelPreview lp, MessageReceivedEvent event, boolean canGoBack, Procedure goBack) {
		CommandsModule.executeCommand((event0, args0) -> {
			GDLevel lvl = (GDLevel) UltimateGDBot.cache().readAndWriteIfNotExists("gd.level." + lp.getId(), () -> {
				BotUtils.typing(event0.getChannel(), true);
				return UltimateGDBot.gdClient().fetch(new GDLevelHttpRequest(lp.getId()));
			});
			
			if (lvl == null)
				throw new GDServersUnavailableException();

			String message = event.getAuthor().mention() + ", here's the level you requested to show.";

			if (canGoBack && UltimateGDBot.isModuleAvailable("reply"))
				message += "\nYou can go back to search results by typing `back`";
			
			int pass = lvl.getPass();
			String upload = lvl.getUploadTimestamp();
			String update = lvl.getLastUpdatedTimestamp();
			
			lvl = new GDLevel(lp, pass, upload, update);
			lvl.setCreatorName(lp.getCreatorName());

			IMessage output = BotUtils.sendMessage(event0.getChannel(), message, GDUtils.buildEmbedForGDLevel(
					AuthorObjects.searchResult(), lvl));

			if (canGoBack) {
				try {
					ReplyModule rm = (ReplyModule) UltimateGDBot.getModule("reply");
					Reply r = new Reply(output, event0.getAuthor(), message0 -> {
						if (message0.getContent().equalsIgnoreCase("back")) {
							goBack.run();
							return true;
						} else
							return false;
					});
					rm.open(r, true, false);
				} catch (ModuleUnavailableException e) {
				}
			}
			
			BotUtils.typing(event0.getChannel(), false);
		}, event, new ArrayList<>());
	}

}
