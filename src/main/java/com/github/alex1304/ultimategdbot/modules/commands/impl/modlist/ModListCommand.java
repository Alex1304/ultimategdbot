package com.github.alex1304.ultimategdbot.modules.commands.impl.modlist;

import java.util.List;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GDMod;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.DatabaseFailureException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.NavigationMenu;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

/**
 * Allows users to see the list of GD moderators
 *
 * @author Alex1304
 */
public class ModListCommand implements Command {
	
	private int page;
	private List<GDMod> mods;
	
	public ModListCommand() {
		this(0, null);
	}
	
	private ModListCommand(int page, List<GDMod> mods) {
		this.page = page;
		this.mods = mods;
	}
	
	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		StringBuffer sb = new StringBuffer(event.getAuthor().mention() + ", here is the latest known list of Geometry Dash moderators:\n\n");
		
		List<GDMod> mods = this.mods;
		
		if (mods == null) {
			mods = DatabaseUtils.query(GDMod.class, "from GDMod");
			if (mods == null)
				throw new DatabaseFailureException();

			mods = mods.stream()
					.map(m -> {
						if (m.getUsername().isEmpty()) {
							GDUser user = (GDUser) UltimateGDBot.cache()
									.readAndWriteIfNotExists("gd.user." + m.getAccountID(), () ->
									UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(m.getAccountID())));
							if (user != null) {
								m.setUsername(user.getName());
								DatabaseUtils.save(m);
							}
						}
						return m;
					})
					.sorted((x, y) -> x.getElder() == y.getElder() ? x.getUsername().isEmpty() || y.getUsername().isEmpty() ?
							(int) (x.getAccountID() - y.getAccountID()) :
								x.getUsername().toLowerCase().compareTo(y.getUsername().toLowerCase()) : x.getElder() ? -1 : 1)
					.collect(Collectors.toList());
		}
		
		final List<GDMod> fMods = mods;
		
		int pageMax = (int) Math.ceil(mods.size() / 20.0) - 1;
		sb.append("Page " + (page + 1) + "/" + (pageMax + 1) + "\n\n");
		
		mods.subList(page * 20, Math.min((page + 1) * 20, mods.size())).forEach(m ->
			sb.append((m.getElder() ? Emojis.ELDER_MOD : Emojis.MOD) + " "
					+ (m.getUsername().isEmpty() ? "Unknown user (" + m.getAccountID() + ")" : m.getUsername()) + "\n"));
		
		NavigationMenu nm = new NavigationMenu(page, pageMax, page0 -> new ModListCommand(page0, fMods), event, args);
		nm.setMenuContent(sb.toString());
		
		CommandsModule.executeCommand(nm, event, args);
	}
}
