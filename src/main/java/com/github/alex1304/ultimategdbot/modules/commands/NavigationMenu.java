package com.github.alex1304.ultimategdbot.modules.commands;

import java.util.List;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * A navigation menu allows to easily make a pagination system on commands with
 * multiple page outputs.
 *
 * @author Alex1304
 */
public class NavigationMenu extends InteractiveMenu {
	
	private String appendMenuEmbedContent;

	public NavigationMenu(int page, int pageMax, Function<Integer, Command> commandForPage, MessageReceivedEvent event, List<String> args) {
		super(false, false);
		
		boolean hasNext = page < pageMax;
		boolean hasPrev = page > 0;
		boolean hasMultiplePages = pageMax > 0;
		
		this.appendMenuEmbedContent = "";
		
		if (hasNext) {
			appendMenuEmbedContent += "To go to next page, type `next`\n";
			this.addSubCommand("next", (event0, args0) -> CommandsModule.executeCommand(commandForPage.apply(page + 1), event, args));
		}

		if (hasPrev) {
			appendMenuEmbedContent += "To go to previous page, type `prev`\n";
			this.addSubCommand("prev", (event0, args0) -> CommandsModule.executeCommand(commandForPage.apply(page - 1), event, args));
		}
		
		if (hasMultiplePages) {
			appendMenuEmbedContent += "To go to a specific page, type `page <number>`, ex. `page 3` or `page 10`";
			this.addSubCommand("page", (event0, args0) -> {
				Procedure rollback = () -> CommandsModule.executeCommand(commandForPage.apply(page), event, args);
				
				if (args0.isEmpty()) {
					rollback.run();
					throw new InvalidCommandArgsException("`page <number>`, ex. `page 3`");
				}
				
				int pageInput = 1;
				
				try {
					pageInput = Integer.parseInt(args0.get(0));
					if (pageInput < 1 || pageInput > pageMax + 1) {
						rollback.run();
						throw new CommandFailedException("Page number is out of range");
					}
				} catch (NumberFormatException e) {
					rollback.run();
					throw new CommandFailedException("Sorry, `" + args0.get(0) + "` isn't a valid page number");
				}
				
				CommandsModule.executeCommand(commandForPage.apply(pageInput - 1), event, args);
			});
		}
		
		super.setMenuEmbedContent(appendMenuEmbedContent);
	}
	
	@Override
	public void setMenuEmbedContent(String content) {
		super.setMenuEmbedContent(content + "\n" + appendMenuEmbedContent);
	}
}
