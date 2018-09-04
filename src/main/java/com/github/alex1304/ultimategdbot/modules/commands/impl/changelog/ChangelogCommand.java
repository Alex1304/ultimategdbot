package com.github.alex1304.ultimategdbot.modules.commands.impl.changelog;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.exceptions.DatabaseFailureException;
import com.github.alex1304.ultimategdbot.exceptions.InvalidCommandArgsException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelChangelogSetting;
import com.github.alex1304.ultimategdbot.utils.BotRoles;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.DatabaseUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RateLimitException;

/**
 * Admin command to post a changelog
 *
 * @author Alex1304
 */
public class ChangelogCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		if (args.size() < 5)
			throw new InvalidCommandArgsException("`" + UltimateGDBot.property("ultimategdbot.prefix.canonical")
					+ "changelog <version> - <title1> | <content1> - <title2> | <content2> ...`");
		
		String version = args.get(0);
		String argsStr = BotUtils.concatCommandArgs(args.subList(1, args.size()));
		String[] items = argsStr.split(" - ");
		Map<String, String> fields = new TreeMap<>();
		int i = 0;
		
		for (String item : items) {
			String[] fieldStr = item.split(" \\| ");
			
			if (fieldStr.length != 2)
				throw new CommandFailedException("The command syntax is wrong near item " + (i + 1));
			
			fields.put(String.format("%02d", i) + fieldStr[0].substring(i == 0 ? 2 : 0), fieldStr[1]);
			i++;
		}
		
		EmbedBuilder eb = new EmbedBuilder();
		
		eb.withTitle(UltimateGDBot.property("ultimategdbot.name") + " " + version + " release notes");
		eb.withAuthorIcon(event.getAuthor().getAvatarURL());
		eb.withAuthorName(BotUtils.formatDiscordUsername(event.getAuthor()).replaceAll("\\\\", ""));
		
		for (Entry<String, String> item : fields.entrySet())
			eb.appendField(item.getKey().substring(2), item.getValue(), false);
		
		eb.withTimestamp(System.currentTimeMillis());
		
		List<GuildSettings> gsList = DatabaseUtils.query(GuildSettings.class, "from GuildSettings g where g.channelChangelog > 0");
		
		if (gsList == null)
			throw new DatabaseFailureException();
		
		List<IChannel> channels = gsList.stream()
				.filter(gs -> {
					Optional<IGuild> og = UltimateGDBot.client().getGuilds().parallelStream()
							.filter(g -> g.getLongID() == gs.getGuildID())
							.findAny();
					if (og.isPresent()) {
						gs.setGuildInstance(og.get());
						return true;
					} else
						return false;
				})
				.map(gs -> new ChannelChangelogSetting(gs).getValue())
				.filter(x -> x != null)
				.collect(Collectors.toList());
		
		EmbedObject embed = eb.build();
		
		// Broadcasting the changelog. Might be moved into an implementation of MessageBroadcaster if the code below works
		
		List<IChannel> remainingToBroadcast = new ArrayList<>(channels);
		
		long start = System.currentTimeMillis();
		BotUtils.typing(event.getChannel(), true);
		
		while (!remainingToBroadcast.isEmpty()) {
			long longestRateLimitTime = 0;
			
			Iterator<IChannel> it = remainingToBroadcast.iterator();
			while (it.hasNext()) {
				IChannel channel = it.next();
				try {
					channel.sendMessage(embed);
					it.remove();
				} catch (RateLimitException e) {
					longestRateLimitTime = Math.max(longestRateLimitTime, e.getRetryDelay());
				} catch (DiscordException e) {
					UltimateGDBot.logException(e);
					it.remove();
				}
			}
			
			try {
				Thread.sleep(longestRateLimitTime);
			} catch (InterruptedException e) {
			}
		}
		
		BotUtils.typing(event.getChannel(), false);
		BotUtils.sendMessage(event.getChannel(), Emojis.SUCCESS + " Sent changelog in " + channels.size()
				+ " guilds in " + BotUtils.formatTimeMillis((System.currentTimeMillis() - start)));
	}
	
	@Override
	public EnumSet<BotRoles> getRolesRequired() {
		return EnumSet.of(BotRoles.OWNER);
	}

}
