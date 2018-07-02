package com.github.alex1304.ultimategdbot.modules.commands.impl.level.internal;

import java.util.List;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.SuperCommand;
import com.github.alex1304.ultimategdbot.modules.commands.impl.level.LevelCommand;

/**
 * Launches an interactive menu with GD level search results
 *
 * @author Alex1304
 */
public class LevelSearchInternalCommand extends SuperCommand {

	public LevelSearchInternalCommand(List<String> levelCmdArgs, GDComponentList<GDLevelPreview> results, int page) {
		super();
		this.setMenuContent(this.buildResultOutput(results, page));
		
		this.addSubCommand("next", (event, args) -> {
			CommandsModule.executeCommand(new LevelCommand(page + 1), event, levelCmdArgs);
		});
		
		this.addSubCommand("prev", (event, args) -> {
			if (page == 0)
				throw new CommandFailedException("You're already in first page");
			CommandsModule.executeCommand(new LevelCommand(page - 1), event, levelCmdArgs);
		});
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
		
		output.append("\n~~To view full info on a level, type the search result number, ex. `1`, `2`, `3`, etc~~ **Unavailable yet**\n");
		output.append("To go to next page of search results, type `next`\n");
		output.append("To go to previous page of search results, type `prev`\n");
		
		return output.toString();
	}

}
