package com.github.alex1304.ultimategdbot.modules.commands.impl.level;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.NavigationMenu;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.GDLevelSearchBuilder;
import com.github.alex1304.ultimategdbot.utils.GDUtils;
import com.github.alex1304.ultimategdbot.utils.Procedure;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;

/**
 * Allows user to search for a level in Geometry Dash
 *
 * @author Alex1304
 */
public class LevelCommand implements Command {
	
	private int page;
	private Function<List<String>, GDLevelSearchBuilder> requestObject;
	private BiFunction<MessageReceivedEvent, List<String>, CommandFailedException> checkArgsValidity;
	
	public LevelCommand() {
		this(0);
	}
	
	public LevelCommand(int page) {
		this(page, args -> {
			GDLevelSearchBuilder lsb = new GDLevelSearchBuilder();
			lsb.setKeywords(BotUtils.concatCommandArgs(args));
			return lsb;
		}, (event, args) -> args.isEmpty() ? new InvalidCommandArgsException("`" + event.getMessage().getContent()
				+ " <name or ID>`, ex `" + event.getMessage().getContent() + " Level Easy`") : null);
	}
	
	public LevelCommand(int page, Function<List<String>, GDLevelSearchBuilder> requestObject,
			BiFunction<MessageReceivedEvent, List<String>, CommandFailedException> checkArgsValidity) {
		this.page = page;
		this.requestObject = requestObject;
		this.checkArgsValidity = checkArgsValidity;
	}

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		CommandFailedException ex = checkArgsValidity.apply(event, args);
		if (ex != null)
			throw ex;

		final Procedure rollBackResults = () -> CommandsModule.executeCommand(this, event, args);
		
		String keywords = BotUtils.concatCommandArgs(args);
		String cacheID = "gd.levelsearch." + keywords + page;

		GDLevelSearchBuilder lsb = this.requestObject.apply(args);
		lsb.setPage(this.page);
		
		GDComponentList<GDLevelPreview> results = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
				.readAndWriteIfNotExists(cacheID, () -> {
					BotUtils.typing(event.getChannel(), true);
					return UltimateGDBot.gdClient().fetch(lsb.build());
				});
		
		if (results == null)
			throw new GDServersUnavailableException();
		
		if (results.isEmpty() && page == 0)
			throw new CommandFailedException("No results found.");
		
		if (results.size() == 1) {
			GDUtils.openLevel(results.get(0), event, false, rollBackResults);
			return;
		}
		
		if (!UltimateGDBot.isModuleAvailable("reply")) {
			EmbedObject em = GDUtils.levelListToEmbed(results, page);
			em.description = "\nAbility to navigate through search results is currently unavailable. Sorry for the inconvenience.";
			BotUtils.sendMessage(event.getChannel(), em);
			return;
		}
		
		NavigationMenu nm = new NavigationMenu(page, 9999, page -> new LevelCommand(page, requestObject, checkArgsValidity), event, args);
		
		nm.setMenuEmbed(GDUtils.levelListToEmbed(results, page));
		nm.setMenuEmbedContent("To view full info on a level, type `select` followed by the position "
				+ "number, ex. `select 2`\n");
		
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
			GDUtils.openLevel(results.get(selectInput - 1), event, true, rollBackResults);
		});
		
		CommandsModule.executeCommand(nm, event, new ArrayList<>());
		BotUtils.typing(event.getChannel(), false);
	}
	
	@Override
	public EnumSet<Permissions> getPermissionsRequired() {
		return EnumSet.of(Permissions.EMBED_LINKS, Permissions.USE_EXTERNAL_EMOJIS);
	}

}
