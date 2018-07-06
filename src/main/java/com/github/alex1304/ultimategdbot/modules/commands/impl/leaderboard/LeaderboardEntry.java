package com.github.alex1304.ultimategdbot.modules.commands.impl.leaderboard;

import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;

import sx.blah.discord.handle.obj.IUser;

/**
 * Represents a leaderboard entry for the leaderboard command
 *
 * @author Alex1304
 */
class LeaderboardEntry implements Comparable<LeaderboardEntry> {

	private Emojis statEmoji;
	private int statValue;
	private GDUser gdUser;
	private IUser discordUser;
	
	public LeaderboardEntry(Emojis statEmoji, int statValue, GDUser gdUser, IUser discordUser) {
		this.statEmoji = statEmoji;
		this.statValue = statValue;
		this.gdUser = gdUser;
		this.discordUser = discordUser;
	}

	/**
	 * Gets the statEmoji
	 *
	 * @return Emojis
	 */
	public Emojis getStatEmoji() {
		return statEmoji;
	}

	/**
	 * Gets the statValue
	 *
	 * @return int
	 */
	public int getStatValue() {
		return statValue;
	}

	/**
	 * Gets the gdUser
	 *
	 * @return GDUser
	 */
	public GDUser getGdUser() {
		return gdUser;
	}

	/**
	 * Gets the discordUser
	 *
	 * @return IUser
	 */
	public IUser getDiscordUser() {
		return discordUser;
	}
	
	@Override
	public String toString() {
		return statEmoji + "  " + statValue + "  -  " + gdUser.getName()
				+ " (" + BotUtils.formatDiscordUsername(discordUser) + ")";
	}

	@Override
	public int compareTo(LeaderboardEntry other) {
		int byValue = other.statValue - this.statValue;
		
		if (byValue == 0)
			return (int) (this.gdUser.getAccountID() - other.gdUser.getAccountID());
		else
			return byValue;
	}
}
