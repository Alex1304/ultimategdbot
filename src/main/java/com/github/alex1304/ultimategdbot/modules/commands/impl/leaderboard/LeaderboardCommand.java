package com.github.alex1304.ultimategdbot.modules.commands.impl.leaderboard;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.api.request.GDUserHttpRequest;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.CommandsModule;
import com.github.alex1304.ultimategdbot.modules.commands.NavigationMenu;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;

/**
 * Executes the leaderboard command
 *
 * @author Alex1304
 */
public class LeaderboardCommand implements Command {
	
	private Function<GDUser, Integer> statFunc;
	private Emojis emoji;
	private int page;

	public LeaderboardCommand(Function<GDUser, Integer> statFunc, Emojis emoji, int page) {
		this.statFunc = statFunc;
		this.emoji = emoji;
		this.page = page;
	}
	
	public LeaderboardCommand(Function<GDUser, Integer> statFunc, Emojis emoji) {
		this(statFunc, emoji, 0);
	}

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		List<UserSettings> usList = DatabaseUtils.query(UserSettings.class, "from UserSettings u where u.linkActivated = true");
		List<IUser> usersInGuild = event.getGuild().getUsers();
		
		BotUtils.typing(event.getChannel(), true);
		
		List<LeaderboardEntry> entries = usList.parallelStream()
				.filter(x -> usersInGuild.stream().anyMatch(user -> user.getLongID() == x.getUserID()))
				.map(x -> {
					GDUser gu = (GDUser) UltimateGDBot.cache()
							.readAndWriteIfNotExistsIgnoreExceptions("gd.user." + x.getGdUserID(), () -> {
								try {
									return UltimateGDBot.gdClient().fetch(new GDUserHttpRequest(x.getGdUserID()));
								} catch (GDAPIException e) {
									return null;
								}
							});
					
					if (gu == null)
						return null;
					
					IUser du = null;
					
					try {
						du = usersInGuild.stream()
							.filter(user -> user.getLongID() == x.getUserID())
							.findAny().get();
					} catch (NoSuchElementException e) {
						return null;
					}
					
					return new LeaderboardEntry(emoji, statFunc.apply(gu), gu, du);
				}).collect(Collectors.toList());
		
		long nulls = entries.stream().filter(x -> x == null).count();
		List<LeaderboardEntry> entryList = entries.stream().filter(x -> x != null).sorted().collect(Collectors.toList());
		
		int pageMax = (int) Math.ceil(entryList.size() / 20.0) - 1;
		
		StringBuffer sb = new StringBuffer("__Geometry Dash leaderboard for server **" + event.getGuild().getName() + "**__\n"
				+ "Total players: " + entryList.size() + ", " + emoji + "leaderboard\n");
		
		if (nulls > 0)
			sb.append(":warning: " + nulls + " players are missing because of errors when fetching their profile\n");
		
		sb.append("\nPage " + (page + 1) + "/" + (pageMax + 1) + "\n\n");
		
		for (int i = page * 20 ; i < entryList.size() && i < page * 20 + 20 ; i++) {
			LeaderboardEntry entry = entryList.get(i);
			String formattedRank = new StringBuffer(BotUtils.truncate(new StringBuffer("" + (i + 1)).reverse().toString(), 3))
					.reverse().toString();
			boolean self = entry.getDiscordUser().equals(event.getAuthor());
			sb.append((self ? "**" : "") + "`#" + formattedRank + "`  |  " + entry.toString() + (self ? "**" : "") + "\n");
		}
		
		sb.append("\nNote that members of this server must have linked their Geometry Dash account to be displayed on this "
				+ "leaderboard. See the command `" + UltimateGDBot.property("ultimategdbot.prefix.canonical") + "account`");
		
		NavigationMenu nm = new NavigationMenu(page, pageMax, page0 -> new LeaderboardCommand(statFunc, emoji, page0), event, args);
		nm.setMenuContent(sb.toString());
		CommandsModule.executeCommand(nm, event, args);
		
		BotUtils.typing(event.getChannel(), false);
	}
	
}
