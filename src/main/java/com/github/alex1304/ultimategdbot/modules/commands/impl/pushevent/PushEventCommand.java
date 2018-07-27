package com.github.alex1304.ultimategdbot.modules.commands.impl.pushevent;

import java.util.EnumSet;
import java.util.List;

import com.github.alex1304.jdash.api.request.GDLevelSearchHttpRequest;
import com.github.alex1304.jdash.api.request.GDTimelyLevelHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.GDTimelyLevel;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.jdashevents.common.CommonEvents;
import com.github.alex1304.jdashevents.customcomponents.GDUpdatedComponent;
import com.github.alex1304.jdashevents.manager.GDEventManager;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.GDServersUnavailableException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.utils.BotRoles;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.Permissions;

/**
 * Allows bot owner to fire any GD event manually
 *
 * @author Alex1304
 */
public class PushEventCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		InteractiveMenu im = new InteractiveMenu();
		im.setMenuContent("Please select the event to fire.");
		
		im.setMenuEmbedContent("To fire a Awarded Level Added event, type `1 <levelID>`\n"
				+ "To fire a Awarded Level Updated event, type `2 <levelID>`\n"
				+ "To fire a Awarded Level Deleted event, type `3 <levelID>`\n"
				+ "To fire a Daily Level Changed event, type `4`\n"
				+ "To fire a Weekly Demon Changed event, type `5`");
		
		im.addSubCommand("1", (event0, args0) -> {
			if (args0.isEmpty())
				throw new InvalidCommandArgsException("`" + event0.getMessage().getContent() + " <level ID>`");
			
			long id = -1;
			
			try {
				id = Long.parseLong(args0.get(0));
			} catch (NumberFormatException e) {
				throw new CommandFailedException("Invalid ID");
			}
			
			final long fid = id;
			GDComponentList<GDLevelPreview> lplist = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.levelsearch." + fid, () ->
							UltimateGDBot.gdClient().fetch(new GDLevelSearchHttpRequest("" + fid, 0)));
			
			if (lplist == null)
				throw new GDServersUnavailableException();
			if (lplist.isEmpty())
				throw new CommandFailedException("Level not found");
			
			GDEventManager.getInstance().dispatch(CommonEvents.AWARDED_LEVEL_ADDED, lplist);
		});
		
		im.addSubCommand("2", (event0, args0) -> {
			if (args0.isEmpty())
				throw new InvalidCommandArgsException("`" + event0.getMessage().getContent() + " <level ID>`");
			
			long id = -1;
			
			try {
				id = Long.parseLong(args0.get(0));
			} catch (NumberFormatException e) {
				throw new CommandFailedException("Invalid ID");
			}
			
			final long fid = id;
			GDComponentList<GDLevelPreview> lplist = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.levelsearch." + fid, () ->
							UltimateGDBot.gdClient().fetch(new GDLevelSearchHttpRequest("" + fid, 0)));
			
			if (lplist == null)
				throw new GDServersUnavailableException();
			if (lplist.isEmpty())
				throw new CommandFailedException("Level not found");
			
			GDComponentList<GDUpdatedComponent<GDLevelPreview>> updated = new GDComponentList<>();
			updated.add(new GDUpdatedComponent<>(null, lplist.get(0)));
			
			GDEventManager.getInstance().dispatch(CommonEvents.AWARDED_LEVEL_UPDATED, updated);
		});
		
		im.addSubCommand("3", (event0, args0) -> {
			if (args0.isEmpty())
				throw new InvalidCommandArgsException("`" + event0.getMessage().getContent() + " <level ID>`");
			
			long id = -1;
			
			try {
				id = Long.parseLong(args0.get(0));
			} catch (NumberFormatException e) {
				throw new CommandFailedException("Invalid ID");
			}
			
			final long fid = id;
			GDComponentList<GDLevelPreview> lplist = (GDComponentList<GDLevelPreview>) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.levelsearch." + fid, () ->
							UltimateGDBot.gdClient().fetch(new GDLevelSearchHttpRequest("" + fid, 0)));
			
			if (lplist == null)
				throw new GDServersUnavailableException();
			if (lplist.isEmpty())
				throw new CommandFailedException("Level not found");
			
			GDEventManager.getInstance().dispatch(CommonEvents.AWARDED_LEVEL_DELETED, lplist);
		});
		
		im.addSubCommand("4", (event0, args0) -> {
			try {
				GDTimelyLevel tl = UltimateGDBot.gdClient().fetch(new GDTimelyLevelHttpRequest(false, UltimateGDBot.gdClient()));
				
				if (tl == null)
					throw new CommandFailedException("Daily level unavailable");
				
				GDEventManager.getInstance().dispatch(CommonEvents.DAILY_LEVEL_CHANGED, new GDUpdatedComponent<>(null, tl));
			} catch (GDAPIException e) {
				throw new GDServersUnavailableException();
			}
		});
		
		im.addSubCommand("5", (event0, args0) -> {
			try {
				GDTimelyLevel tl = UltimateGDBot.gdClient().fetch(new GDTimelyLevelHttpRequest(true, UltimateGDBot.gdClient()));
				
				if (tl == null)
					throw new CommandFailedException("Weekly demon unavailable");
				
				GDEventManager.getInstance().dispatch(CommonEvents.WEEKLY_DEMON_CHANGED, new GDUpdatedComponent<>(null, tl));
			} catch (GDAPIException e) {
				throw new GDServersUnavailableException();
			}
		});
		
		CommandsModule.executeCommand(im, event, args);
	}
	
	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.OWNER);
	}
	
	@Override
	public EnumSet<Permissions> getPermissionsRequired() {
		return EnumSet.of(Permissions.EMBED_LINKS);
	}
}
