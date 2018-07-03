package com.github.alex1304.ultimategdbot.modules.commands.impl.level;

import java.util.ArrayList;
import java.util.List;

import com.github.alex1304.jdash.api.request.GDLevelSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.NavigationMenu;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

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
		
		try {
			String keywords = BotUtils.concatCommandArgs(args);
			String cacheID = "gd.levelsearch." + keywords + page;
				
			
			GDComponentList<GDLevelPreview> results = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
					.readAndWriteIfNotExists(cacheID, () -> {
						BotUtils.typing(event.getChannel(), true);
						return UltimateGDBot.gdClient().fetch(new GDLevelSearchHttpRequest(keywords, page));
					});
			
			if (!UltimateGDBot.isModuleAvailable("reply")) {
				BotUtils.sendMessage(event.getChannel(), buildResultOutput(results, page) +
						"\nAbility to navigate through search results is currently unavailable. Sorry for the inconvenience.");
				return;
			}
			
			NavigationMenu nm = new NavigationMenu(buildResultOutput(results, page)
					+ "\n~~To view full info on a level, type the search result number, ex. `1`, `2`, `3`, etc~~ **Unavailable yet**");
			
			nm.setOnNext((event0, args0) -> {
				CommandsModule.executeCommand(new LevelCommand(page + 1), event, args);
			});
			nm.setOnPrev((event0, args0) -> {
				CommandsModule.executeCommand(new LevelCommand(page - 1), event, args);
			});
			nm.setOnPage((event0, args0) -> {
				Procedure rollBackResults = () -> CommandsModule.executeCommand(new LevelCommand(page), event, args);
				
				if (args0.isEmpty()) {
					rollBackResults.run();
					throw new InvalidCommandArgsException("`page <number>`, ex. `page 3`");
				}
				
				int pageInput = 1;
				
				try {
					pageInput = Integer.parseInt(args0.get(0));
					if (pageInput < 1) {
						rollBackResults.run();
						throw new CommandFailedException("Page number cannot be negative");
					}
				} catch (NumberFormatException e) {
					rollBackResults.run();
					throw new CommandFailedException("Sorry, `" + args0.get(0) + "` isn't a valid page number");
				}
				
				CommandsModule.executeCommand(new LevelCommand(pageInput - 1), event, args);
			});
			
			CommandsModule.executeCommand(nm, event, new ArrayList<>());
		} catch(GDAPIException e) {
			throw new GDServersUnavailableException();
		} catch (Exception e) {
			throw new RuntimeException();
		} finally {
			BotUtils.typing(event.getChannel(), false);
		}
	}

	private String buildResultOutput(GDComponentList<GDLevelPreview> results, int page) {
		StringBuffer output = new StringBuffer();
		output.append("Page: ");
		output.append(page + 1);
		output.append("\n\n");
		
		int i = 1;
		for (GDLevelPreview lp : results) {
			output.append(String.format("`%d` - __**%s**__ by **%s**\n"
					+ "      ID: %d\n"
					+ "      Song: %s\n",
					i,
					lp.getName(),
					lp.getCreatorName(),
					lp.getId(),
					lp.getSongTitle()));
			i++;
		}
		
		return output.toString();
	}

}
