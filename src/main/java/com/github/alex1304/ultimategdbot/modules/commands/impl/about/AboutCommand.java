package com.github.alex1304.ultimategdbot.modules.commands.impl.about;

import java.util.EnumSet;
import java.util.List;
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
		
		String content = "**Thank you for using UltimateGDBot v" + UltimateGDBot.property("ultimategdbot.release.version") + " (" + UltimateGDBot.property("ultimategdbot.release.channel") + " build)**\n\n"
				+ "UltimateGDBot is a Discord bot developed by **" + BotUtils.formatDiscordUsername(UltimateGDBot.owner()) + "** and designed for Geometry Dash players. It gives users an easy access to data "
				+ "from Geometry Dash servers (levels, user stats, etc), and can notify them on events happening in-game "
				+ "(new featured levels, new Daily levels/Weekly demons, new Geometry Dash moderators...).\n\n"
				+ "UltimateGDBot is currently operating in **" + UltimateGDBot.client().getGuilds().size()
				+ "** servers which represents **" + UltimateGDBot.client().getUsers().size() + "** users across Discord. "
				+ "You can invite UltimateGDBot to your own server using the following authorization link: <" + bib.build() + ">\n"
				+ "\n"
				+ "UltimateGDBot is an open-source project. You can check out the GitHub repository at "
				+ "https://github.com/Alex1304/ultimategdbot" + (invite != null && invite.isPresent() ? " and join the official "
						+ "support server at https://" + invite.get().toString() : "") + "\n"
				+ "User manual: http://ultimategdbot.readthedocs.io/en/latest"
				+ "\n───────────────────\n\n"
				+ "UltimateGDBot is hosted and powered by __FastVM__, a company that sells powerful virtual private servers "
				+ "for a low price. Check their plans on their website https://www.fastvm.io/ and use promo code `GDBOT` "
				+ "to get 10% discount.";
		
		BotUtils.sendMessage(event.getChannel(), content);
	}
}
