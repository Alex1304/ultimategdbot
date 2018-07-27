package com.github.alex1304.ultimategdbot.modules.commands.impl.about;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.exceptions.CommandFailedException;
import com.github.alex1304.ultimategdbot.modules.commands.Command;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IExtendedInvite;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.BotInviteBuilder;
import sx.blah.discord.util.MissingPermissionsException;

/**
 * Allows users to get info about the bot.
 *
 * @author Alex1304
 */
public class AboutCommand implements Command {

	@Override
	public void runCommand(MessageReceivedEvent event, List<String> args) throws CommandFailedException {
		BotInviteBuilder bib = new BotInviteBuilder(UltimateGDBot.client());
		bib.withClientID(UltimateGDBot.property("ultimategdbot.client.id"));
		bib.withPermissions(EnumSet.of(
				Permissions.SEND_MESSAGES,
				Permissions.EMBED_LINKS,
				Permissions.USE_EXTERNAL_EMOJIS,
				Permissions.READ_MESSAGE_HISTORY,
				Permissions.READ_MESSAGES,
				Permissions.MANAGE_ROLES
		));
		
		Optional<IExtendedInvite> invite = null;
		
		try {
			List<IExtendedInvite> invites = UltimateGDBot.officialGuild().getExtendedInvites();
			if (invites == null)
				throw new NullPointerException("Got a null invite list from the official server");
			invite = invites.stream()
					.filter(x -> x != null && x.getInviter() != null && x.getInviter().equals(UltimateGDBot.owner()))
					.findAny();
		} catch (MissingPermissionsException e) {
			UltimateGDBot.logError("[AboutCommand] Error when fetching invite from official server : " + e.getMessage());
		}
		
		if (invite == null || !invite.isPresent())
			UltimateGDBot.logWarning("[AboutCommand] Cannot find any Discord invite links for the official server");
		
		try {
			Map<String, String> vars = new HashMap<>();
			
			vars.put("bot_name", UltimateGDBot.property("ultimategdbot.name"));
			vars.put("project_version", UltimateGDBot.property("ultimategdbot.release.version"));
			vars.put("bot_release_channel", UltimateGDBot.property("ultimategdbot.release.channel"));
			vars.put("bot_owner", BotUtils.formatDiscordUsername(UltimateGDBot.owner()));
			vars.put("server_count", "" + UltimateGDBot.client().getGuilds().size());
			vars.put("user_count", "" + UltimateGDBot.client().getUsers().size());
			vars.put("bot_auth_link", bib.build());
			vars.put("official_guild_invite", invite == null || !invite.isPresent() ? "*[no invite links available]*" : "https://" + invite.get().toString());
			
			InputStream is = getClass().getResourceAsStream("/about.txt");
			BufferedReader sr = new BufferedReader(new InputStreamReader(is));
			
			StringBuilder sb = new StringBuilder();
			sr.lines().forEach(line -> sb.append(line + "\n"));
			sr.close();
			
			String content = sb.toString();
	
			for (Entry<String, String> var : vars.entrySet())
				content = content.replaceAll("\\{\\{ *" + String.valueOf(var.getKey()) + " *\\}\\}", String.valueOf(var.getValue()));
			
			BotUtils.sendMessage(event.getChannel(), content);
		} catch (IOException | UncheckedIOException e) {
			throw new CommandFailedException("Something went wrong. Try again later.");
		}
	}
}
