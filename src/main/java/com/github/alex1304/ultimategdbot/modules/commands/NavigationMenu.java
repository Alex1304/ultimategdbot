package com.github.alex1304.ultimategdbot.modules.commands;

import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;

/**
 * A navigation menu allows to easily make a pagination system on commands with
 * multiple page outputs.
 *
 * @author Alex1304
 */
public class NavigationMenu extends InteractiveMenu {

	public NavigationMenu(String content, String embedContent) {
		this.setMenuContent(content);
		this.setMenuEmbedContent(embedContent
				+ "To go to next page, type `next`\n"
				+ "To go to previous page, type `prev`\n"
				+ "To go to a specific page, type `page <number>`, ex. `page 3` or `page 10`");
		
		this.addSubCommand("next", (event, args) -> {
			throw new CommandFailedException("No action is bound to this instruction");
		});
		
		this.addSubCommand("prev", (event, args) -> {
			throw new CommandFailedException("No action is bound to this instruction");
		});
		
		this.addSubCommand("page", (event, args) -> {
			throw new CommandFailedException("No action is bound to this instruction");
		});
	}
	
	/**
	 * Command to execute when going to next page
	 * 
	 * @param cmd
	 */
	public void setOnNext(Command cmd) {
		this.addSubCommand("next", cmd);
	}
	
	/**
	 * Command to execute when going to previous page
	 * 
	 * @param cmd
	 */
	public void setOnPrev(Command cmd) {
		this.addSubCommand("prev", cmd);
	}
	
	/**
	 * Command to execute when going to a specific page
	 * 
	 * @param cmd
	 */
	public void setOnPage(Command cmd) {
		this.addSubCommand("page", cmd);
	}

}
