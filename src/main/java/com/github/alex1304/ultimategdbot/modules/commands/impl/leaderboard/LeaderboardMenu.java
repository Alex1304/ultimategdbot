package com.github.alex1304.ultimategdbot.modules.commands.impl.leaderboard;

import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.modules.commands.InteractiveMenu;
import com.github.alex1304.ultimategdbot.utils.Emojis;

/**
 * The leaderboard command shows all users in the server that have a GD account
 * linked, and sorts them according to their stats (stars, demons, diamonds,
 * secret coins, user coins, cp, depending on the user's choice)
 *
 * @author Alex1304
 */
public class LeaderboardMenu extends InteractiveMenu {

	public LeaderboardMenu() {
		this.setMenuContent("**Compare your stats with other players in this server by "
				+ "showing a server-wide Geometry Dash leaderboard!**\n\n"
				+ "To get started, select which type of leaderboard you want to show:");
		this.setMenuEmbedContent("To show " + Emojis.STAR + " Stars leaderboard, type `stars`\n"
				+ "To show " + Emojis.DIAMOND + " Diamonds leaderboard, type `diamonds`\n"
				+ "To show " + Emojis.USER_COIN + " User Coins leaderboard, type `ucoins`\n"
				+ "To show " + Emojis.SECRET_COIN + " Secret Coins leaderboard, type `scoins`\n"
				+ "To show " + Emojis.DEMON + " Demons leaderboard, type `demons`\n"
				+ "To show " + Emojis.CREATOR_POINTS + " Creator Points leaderboard, type `cp`\n");
		
		
		this.addSubCommand("stars", new LeaderboardCommand(GDUser::getStars, Emojis.STAR));
		this.addSubCommand("diamonds", new LeaderboardCommand(GDUser::getDiamonds, Emojis.DIAMOND));
		this.addSubCommand("ucoins", new LeaderboardCommand(GDUser::getUserCoins, Emojis.USER_COIN));
		this.addSubCommand("scoins", new LeaderboardCommand(GDUser::getSecretCoins, Emojis.SECRET_COIN));
		this.addSubCommand("demons", new LeaderboardCommand(GDUser::getDemons, Emojis.DEMON));
		this.addSubCommand("cp", new LeaderboardCommand(GDUser::getCreatorPoints, Emojis.CREATOR_POINTS));
	}

}
