package com.github.alex1304.ultimategdbot.modules.commands.impl.featuredinfo;

import java.util.List;

import com.github.alex1304.jdash.api.request.GDLevelHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevel;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.jdash.util.Constants;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.GDLevelSearchBuilder;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows users to browse the Featured levels in the game. They can find info on the placement, 
 * and quickly see the position of a level in the featured section.
 *
 * @author Alex1304
 */
public class FeaturedInfoCommand implements Command {
	
	public static final int START_PAGE = 10_000;

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (args.isEmpty())
			throw new InvalidCommandArgsException("`" + event.getMessage().getContent()
				+ " <level ID>`, ex `" + event.getMessage().getContent() + " 12345678`");
		
		long levelID = -1;
		
		try {
			levelID = Long.parseLong(args.get(0));
		} catch (NumberFormatException e) {
			throw new CommandFailedException("Invalid ID");
		}
		
		final long fLevelID = levelID;
		
		GDLevel lvl = (GDLevel) UltimateGDBot.cache()
				.readAndWriteIfNotExists("gd.level." + levelID, () ->
						UltimateGDBot.gdClient().fetch(new GDLevelHttpRequest(fLevelID)));
		
		if (lvl == null)
			throw new CommandFailedException("Level not found");
		
		if (lvl.getFeaturedScore() == 0)
			throw new CommandFailedException("This level isn't in the Featured section.");
		
		FeaturedLevel flvl = null;
		
		long score = lvl.getFeaturedScore();
		int page = START_PAGE;
		boolean linearMode = false;
		int pageMin = 0, pageMax = Integer.MAX_VALUE;
		
		BotUtils.typing(event.getChannel(), true);
		
		while (flvl == null) {
			GDLevelSearchBuilder lsb = new GDLevelSearchBuilder();
			lsb.setType(Constants.LEVEL_SEARCH_TYPE_FEATURED);
			lsb.setPage(page);
			lsb.setKeywords("-");
			
			try {
				GDComponentList<GDLevelPreview> pageResults = UltimateGDBot.gdClient().fetch(lsb.build());
				
				if (!linearMode && (pageResults == null || pageResults.isEmpty() || score >= pageResults.get(0).getFeaturedScore())) {
					pageMax = page;
					page -= Math.max(1, (pageMax - pageMin) / 2);
				} else if (!linearMode && (score < pageResults.get(pageResults.size() - 1).getFeaturedScore())) {
					pageMin = page;
					page += Math.max(1, (pageMax - pageMin) / 2);
				} else {
					for (int i = 0 ; flvl == null && i < pageResults.size() ; i++) {
						GDLevelPreview i_ = pageResults.get(i);
						if (i_.getId() == lvl.getId())
							flvl = new FeaturedLevel(page, i, i_);
					}
					
					if (flvl == null) {
						page++;
						linearMode = true;
					}
				}
				
				if (page < 0 || (linearMode && (pageResults == null || pageResults.isEmpty())))
					throw new CommandFailedException("This level could not be found in the Featured section.");
				
			} catch (GDAPIException e) {
				throw new GDServersUnavailableException();
			}
		}

		BotUtils.typing(event.getChannel(), false);
		
		BotUtils.sendMessage(event.getChannel(), "**__" + flvl.getLevel().getName() + "__ by "
				+ flvl.getLevel().getCreatorName() + "** (" + flvl.getLevel().getId() + ") is currently placed"
				+ " in page **" + (flvl.getPage() + 1) + "** of the Featured section at position " + (flvl.getPosition() + 1));
		
	}
	
	/**
	 * Object representation of a featured level
	 *
	 * @author Alex1304
	 */
	class FeaturedLevel {
		private int page;
		private int position;
		private GDLevelPreview level;
		
		public FeaturedLevel(int page, int position, GDLevelPreview level) {
			this.page = page;
			this.position = position;
			this.level = level;
		}

		/**
		 * Gets the page
		 *
		 * @return int
		 */
		public int getPage() {
			return page;
		}
		
		/**
		 * Gets the position
		 *
		 * @return int
		 */
		public int getPosition() {
			return position;
		}
		
		/**
		 * Sets the page
		 *
		 * @param page - int
		 */
		public void setPage(int page) {
			this.page = page;
		}
		
		/**
		 * Sets the position
		 *
		 * @param position - int
		 */
		public void setPosition(int position) {
			this.position = position;
		}

		/**
		 * Gets the level
		 *
		 * @return GDLevelPreview
		 */
		public GDLevelPreview getLevel() {
			return level;
		}

		/**
		 * Sets the level
		 *
		 * @param level - GDLevelPreview
		 */
		public void setLevel(GDLevelPreview level) {
			this.level = level;
		}
	}

}
