package com.github.alex1304.ultimategdbot.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.api.request.GDUserSearchHttpRequest;
import com.github.alex1304.jdash.component.GDComponentList;
import com.github.alex1304.jdash.component.GDLevel;
import com.github.alex1304.jdash.component.GDLevelPreview;
import com.github.alex1304.jdash.component.GDSong;
import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.jdash.component.GDUserPreview;
import com.github.alex1304.jdash.component.property.GDUserRole;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.UserSettings;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

/**
 * Provides utility methods for Geometry Dash related features of the bot
 *
 * @author Alex1304
 */
public class GDUtils {

	public static Map<String, String> difficultyIconByName = new HashMap<>();
	public static Map<Integer, String> formatGameVersion = new HashMap<>();

	static {
		difficultyIconByName.put("6-harder-featured", "https://i.imgur.com/b7J4AXi.png");
		difficultyIconByName.put("0-insane-epic", "https://i.imgur.com/GdS2f8f.png");
		difficultyIconByName.put("0-harder", "https://i.imgur.com/5lT74Xj.png");
		difficultyIconByName.put("4-hard-epic", "https://i.imgur.com/toyo1Cd.png");
		difficultyIconByName.put("4-hard", "https://i.imgur.com/XnUynAa.png");
		difficultyIconByName.put("6-harder", "https://i.imgur.com/e499HCB.png");
		difficultyIconByName.put("5-hard-epic", "https://i.imgur.com/W11eyJ9.png");
		difficultyIconByName.put("6-harder-epic", "https://i.imgur.com/9x1ddvD.png");
		difficultyIconByName.put("5-hard", "https://i.imgur.com/Odx0nAT.png");
		difficultyIconByName.put("1-auto-featured", "https://i.imgur.com/DplWGja.png");
		difficultyIconByName.put("5-hard-featured", "https://i.imgur.com/HiyX5DD.png");
		difficultyIconByName.put("8-insane-featured", "https://i.imgur.com/PYJ5T0x.png");
		difficultyIconByName.put("0-auto-featured", "https://i.imgur.com/eMwuWmx.png");
		difficultyIconByName.put("8-insane", "https://i.imgur.com/RDVJDaO.png");
		difficultyIconByName.put("7-harder-epic", "https://i.imgur.com/X3N5sm1.png");
		difficultyIconByName.put("0-normal-epic", "https://i.imgur.com/VyV8II6.png");
		difficultyIconByName.put("0-demon-hard-featured", "https://i.imgur.com/lVdup3A.png");
		difficultyIconByName.put("8-insane-epic", "https://i.imgur.com/N2pjW2W.png");
		difficultyIconByName.put("3-normal-epic", "https://i.imgur.com/S3PhlDs.png");
		difficultyIconByName.put("0-normal-featured", "https://i.imgur.com/Q1MYgu4.png");
		difficultyIconByName.put("2-easy", "https://i.imgur.com/yG1U6RP.png");
		difficultyIconByName.put("0-hard-featured", "https://i.imgur.com/8DeaxfL.png");
		difficultyIconByName.put("0-demon-hard-epic", "https://i.imgur.com/xLFubIn.png");
		difficultyIconByName.put("1-auto", "https://i.imgur.com/Fws2s3b.png");
		difficultyIconByName.put("0-demon-hard", "https://i.imgur.com/WhrTo7w.png");
		difficultyIconByName.put("0-easy", "https://i.imgur.com/kWHZa5d.png");
		difficultyIconByName.put("2-easy-featured", "https://i.imgur.com/Kyjevk1.png");
		difficultyIconByName.put("0-insane-featured", "https://i.imgur.com/t8JmuIw.png");
		difficultyIconByName.put("0-hard", "https://i.imgur.com/YV4Afz2.png");
		difficultyIconByName.put("0-na", "https://i.imgur.com/T3YfK5d.png");
		difficultyIconByName.put("7-harder", "https://i.imgur.com/dJoUDUk.png");
		difficultyIconByName.put("0-na-featured", "https://i.imgur.com/C4oMYGU.png");
		difficultyIconByName.put("3-normal", "https://i.imgur.com/cx8tv98.png");
		difficultyIconByName.put("0-harder-featured", "https://i.imgur.com/n5kA2Tv.png");
		difficultyIconByName.put("0-harder-epic", "https://i.imgur.com/Y7bgUu9.png");
		difficultyIconByName.put("0-na-epic", "https://i.imgur.com/hDBDGzX.png");
		difficultyIconByName.put("1-auto-epic", "https://i.imgur.com/uzYx91v.png");
		difficultyIconByName.put("0-easy-featured", "https://i.imgur.com/5p9eTaR.png");
		difficultyIconByName.put("0-easy-epic", "https://i.imgur.com/k2lJftM.png");
		difficultyIconByName.put("0-hard-epic", "https://i.imgur.com/SqnA9kJ.png");
		difficultyIconByName.put("3-normal-featured", "https://i.imgur.com/1v3p1A8.png");
		difficultyIconByName.put("0-normal", "https://i.imgur.com/zURUazz.png");
		difficultyIconByName.put("6-harder-featured", "https://i.imgur.com/b7J4AXi.png");
		difficultyIconByName.put("2-easy-epic", "https://i.imgur.com/wl575nH.png");
		difficultyIconByName.put("7-harder-featured", "https://i.imgur.com/v50cZBZ.png");
		difficultyIconByName.put("0-auto", "https://i.imgur.com/7xI8EOp.png");
		difficultyIconByName.put("0-insane", "https://i.imgur.com/PeOvWuq.png");
		difficultyIconByName.put("4-hard-featured", "https://i.imgur.com/VW4yufj.png");
		difficultyIconByName.put("0-auto-epic", "https://i.imgur.com/QuRBnpB.png");
		difficultyIconByName.put("10-demon-hard", "https://i.imgur.com/jLBD7cO.png");
		difficultyIconByName.put("9-insane-featured", "https://i.imgur.com/byhPbgR.png");
		difficultyIconByName.put("10-demon-hard-featured", "https://i.imgur.com/7deDmTQ.png");
		difficultyIconByName.put("10-demon-hard-epic", "https://i.imgur.com/xtrTl4r.png");
		difficultyIconByName.put("9-insane", "https://i.imgur.com/5VA2qDb.png");
		difficultyIconByName.put("9-insane-epic", "https://i.imgur.com/qmfey5L.png");

		// Demon difficulties
		difficultyIconByName.put("0-demon-medium-epic", "https://i.imgur.com/eEEzM6I.png");
		difficultyIconByName.put("10-demon-medium-epic", "https://i.imgur.com/ghco42q.png");
		difficultyIconByName.put("10-demon-insane", "https://i.imgur.com/nLZqoyQ.png");
		difficultyIconByName.put("0-demon-extreme-epic", "https://i.imgur.com/p250YUh.png");
		difficultyIconByName.put("0-demon-easy-featured", "https://i.imgur.com/r2WNVw0.png");
		difficultyIconByName.put("10-demon-easy", "https://i.imgur.com/0zM0VuT.png");
		difficultyIconByName.put("10-demon-medium", "https://i.imgur.com/lvpPepA.png");
		difficultyIconByName.put("10-demon-insane-epic", "https://i.imgur.com/2BWY8pO.png");
		difficultyIconByName.put("10-demon-medium-featured", "https://i.imgur.com/kkAZv5O.png");
		difficultyIconByName.put("0-demon-extreme-featured", "https://i.imgur.com/4MMF8uE.png");
		difficultyIconByName.put("0-demon-extreme", "https://i.imgur.com/v74cX5I.png");
		difficultyIconByName.put("0-demon-medium", "https://i.imgur.com/H3Swqhy.png");
		difficultyIconByName.put("0-demon-medium-featured", "https://i.imgur.com/IaeyGY4.png");
		difficultyIconByName.put("0-demon-insane", "https://i.imgur.com/fNC1iFH.png");
		difficultyIconByName.put("0-demon-easy-epic", "https://i.imgur.com/idesUcS.png");
		difficultyIconByName.put("10-demon-easy-epic", "https://i.imgur.com/wUGOGJ7.png");
		difficultyIconByName.put("10-demon-insane-featured", "https://i.imgur.com/RWqIpYL.png");
		difficultyIconByName.put("10-demon-easy-featured", "https://i.imgur.com/fFq5lbN.png");
		difficultyIconByName.put("0-demon-insane-featured", "https://i.imgur.com/1MpbSRR.png");
		difficultyIconByName.put("0-demon-insane-epic", "https://i.imgur.com/ArGfdeh.png");
		difficultyIconByName.put("10-demon-extreme", "https://i.imgur.com/DEr1HoM.png");
		difficultyIconByName.put("0-demon-easy", "https://i.imgur.com/45GaxRN.png");
		difficultyIconByName.put("10-demon-extreme-epic", "https://i.imgur.com/gFndlkZ.png");
		difficultyIconByName.put("10-demon-extreme-featured", "https://i.imgur.com/xat5en2.png");
		
		formatGameVersion.put(10, "1.7");
		formatGameVersion.put(11, "1.8");
	}

	/**
	 * Builds an embed for the specified Geometry Dash level
	 * 
	 * @param authorName
	 *            - authorName field of the embed
	 * @param authorIcon
	 *            - authorIcon field of the embed
	 * @param lvl
	 *            - the level to convert to embed
	 * @param lp
	 *            - the level to convert to embed
	 * @return an EmbedObject representing the embedded level
	 */
	public static EmbedObject buildEmbedForGDLevel(String authorName, String authorIcon, GDLevelPreview lp) {
		EmbedBuilder eb = new EmbedBuilder();
		
		GDLevel lvl = null;
		
		if (lp instanceof GDLevel)
			lvl = (GDLevel) lp;

		eb.withAuthorName(authorName);
		eb.withAuthorIcon(authorIcon);
		eb.withThumbnail(getDifficultyImageForLevel(lp));
		
		eb.appendField(Emojis.PLAY + "  __" + lp.getName() + "__ by " + lp.getCreatorName() + "",
				"**Description:** " + (lp.getDescription().isEmpty() ? "*(No description provided)*" : BotUtils.escapeMarkdown(lp.getDescription())), true);
		eb.appendField("Coins: " + coinsToEmoji(lp.getCoinCount(), lp.hasCoinsVerified(), false),
				Emojis.DOWNLOADS + " " + lp.getDownloads() + "\t\t"
						+ (lp.getLikes() < 0 ? Emojis.DISLIKE + " " : Emojis.LIKE + " ") + lp.getLikes() + "\t\t"
						+ Emojis.LENGTH + " " + lp.getLength().toString().toUpperCase() + "\n"
				+ "───────────────────\n", false);
		
		
		String objCount = "**Object count:** ";
		if (lp.getObjectCount() > 0 || lp.getLevelVersion() >= 21) {
			if (lp.getObjectCount() == 65535)
				objCount += "More than ";
			objCount += lp.getObjectCount();
		} else
			objCount += "_Info unavailable for levels playable in GD 2.0 or older_";
		objCount += "\n";
		
		StringBuffer sb = new StringBuffer();
		sb.append(formatSongSecondaryMetadata(lp.getSong()) + "\n");
		sb.append("───────────────────\n");
		sb.append("**Level ID:** " + lp.getId() + "\n");
		sb.append("**Level version:** " + lp.getLevelVersion() + "\n");
		sb.append("**Minimum GD version required to play this level:** " + formatGameVersion(lp.getGameVersion()) + "\n");
		sb.append(objCount);
		
		if (lvl != null) {
			String pass = "";
				if (lvl.getPass() == -2)
					pass = "Yes, no passcode required";
				else if (lvl.getPass() == -1)
					pass = "No";
				else
					pass = "Yes, " + Emojis.LOCK + " passcode: " + String.format("%06d", lvl.getPass());
			sb.append("**Copyable:** " + pass + "\n");
			sb.append("**Uploaded:** " + lvl.getUploadTimestamp() + " ago\n");
			sb.append("**Last updated:** " + lvl.getLastUpdatedTimestamp() + " ago\n");
		}
		
		if (lp.getOriginalLevelID() > 0 || lp.getFeaturedScore() > 0 || lp.getObjectCount() > 40000)
			sb.append("───────────────────\n");
		if (lp.getOriginalLevelID() > 0)
			sb.append(Emojis.COPY + " This level is a copy of " + lp.getOriginalLevelID() + "\n");
		if (lp.getObjectCount() > 40000)
			sb.append(Emojis.OBJECT_OVERFLOW + " **This level may lag on low end devices**\n");
		if (lp.getFeaturedScore() > 0)
			sb.append(Emojis.ICON_NA_FEATURED + "This level has been placed in the Featured section with a score of **"
						+ lp.getFeaturedScore() + "** (the higher this score is, the higher it's placed in the Featured section)\n");
		
		eb.appendField(":musical_note:   " + formatSongPrimaryMetadata(lp.getSong()), sb.toString(), false);

		return eb.build();
	}
	
	/**
	 * Builds an embed for the specified Geometry Dash user
	 * @param authorName - authorName field of the embed
	 * @param authorIcon - authorIcon field of the embed
	 * @param user - the user to convert to embed
	 * @return an EmbedObject representing the embedded user
	 */
	public static EmbedObject buildEmbedForGDUser(String authorName, String authorIcon, GDUser user) {
		EmbedBuilder eb = new EmbedBuilder();
		
		eb.withAuthorName(authorName);
		eb.withAuthorIcon(authorIcon);
		
		eb.appendField(":chart_with_upwards_trend:  " + user.getName() + "'s stats", Emojis.STAR + "  " + user.getStars()+ "\t\t"
			+ Emojis.DIAMOND + "  " + user.getDiamonds() + "\t\t"
			+ Emojis.USER_COIN + "  " + user.getUserCoins() + "\t\t"
			+ Emojis.SECRET_COIN + "  " + user.getSecretCoins() + "\t\t"
			+ Emojis.DEMON + "  " + user.getDemons() + "\t\t"
			+ Emojis.CREATOR_POINTS + "  " + user.getCreatorPoints() + "\n", false);
		
		String mod = "";
		if (user.getRole() == GDUserRole.MODERATOR)
			mod = Emojis.MOD + "  **" + user.getRole().toString().replaceAll("_", " ") + "**\n";
		else if (user.getRole() == GDUserRole.ELDER_MODERATOR)
			mod = Emojis.ELDER_MOD + "  **" + user.getRole().toString().replaceAll("_", " ") + "**\n";
		
		String discord = "";
		List<IUser> linkedAccounts = getDiscordUsersLinkedToGDAccount(user.getAccountID());
		if (!linkedAccounts.isEmpty()) {
			discord += Emojis.DISCORD + "  **Discord:** ";
			for (IUser u : linkedAccounts)
				discord += BotUtils.formatDiscordUsername(u) + ", ";
			discord = discord.substring(0, discord.length() - 2);
		}
		
		try {
			eb.appendField("───────────────────", mod
					+ Emojis.GLOBAL_RANK + "  **Global Rank:** "
					+ (user.getGlobalRank() == 0 ? "*Unranked*" : user.getGlobalRank()) + "\n"
					+ Emojis.YOUTUBE + "  **Youtube:** "
						+ (user.getYoutube().isEmpty() ? "*not provided*" : "[Open link](https://www.youtube.com/channel/"
						+ URLEncoder.encode(user.getYoutube(), "UTF-8") + ")") + "\n"
					+ Emojis.TWITCH + "  **Twitch:** "
						+ (user.getTwitch().isEmpty() ? "*not provided*" : "["  + user.getTwitch()
						+ "](http://www.twitch.tv/" + URLEncoder.encode(user.getTwitch(), "UTF-8") + ")") + "\n"
					+ Emojis.TWITTER + "  **Twitter:** "
						+ (user.getTwitter().isEmpty() ? "*not provided*" : "[@" + user.getTwitter() + "]"
						+ "(http://www.twitter.com/" + URLEncoder.encode(user.getTwitter(), "UTF-8") + ")") + "\n"
					+ discord, false);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		eb.withFooterText("PlayerID: " + user.getPlayerID() + " | " + "AccountID: " + user.getAccountID());
		
		return eb.build();
	}
	
	/**
	 * Builds a string of emojis according to the coin count and their verified status
	 * 
	 * @param n - the coin count
	 * @param verified - verified status of the coins
	 * @param shorten - short or long version of the string
	 * @return
	 */
	public static String coinsToEmoji(int n, boolean verified, boolean shorten) {
		String emoji = "" + (verified ? Emojis.USER_COIN : Emojis.USER_COIN_UNVERIFIED);
		StringBuffer output = new StringBuffer();
		
		if (shorten) {
			if (n <= 0)
				return "";
			
			output.append(emoji);
			output.append(" x");
			output.append(n);
		} else {
			if (n <= 0)
				return "None";
			
			for (int i = 1 ; i <= n && i <= 3 ; i++) {
				output.append(emoji);
				output.append(" ");
			}
		}
		
		return output.toString();
	}

	private static String getDifficultyImageForLevel(GDLevelPreview lvl) {
		String difficulty = "";

		difficulty += lvl.getStars() + "-";
		if (lvl.isDemon())
			difficulty += "demon-" + lvl.getDemonDifficulty().toString().toLowerCase();
		else if (lvl.isAuto())
			difficulty += "auto";
		else
			difficulty += lvl.getDifficulty().toString().toLowerCase();
		if (lvl.isEpic())
			difficulty += "-epic";
		else if (lvl.getFeaturedScore() > 0)
			difficulty += "-featured";

		return difficultyIconByName.get(difficulty);
	}
	
	/**
	 * Adds a dot before the last difit of the version number. For example, 20
	 * will become 2.0, etc.
	 * 
	 * @param v
	 *            - the int representing the version
	 * @return String - the formatted version
	 */
	public static String formatGameVersion(int v) {
		if (v < 10)
			return "<1.6";
		if (formatGameVersion.containsKey(v))
			return formatGameVersion.get(v);
		
		String vStr = String.format("%02d", v);
		if (vStr.length() <= 1)
			return vStr;
		
		return vStr.substring(0, vStr.length() - 1) + "." + vStr.charAt(vStr.length() - 1);
	}
	
	/**
	 * Formats song primary metadata (title + author) to a human-readable format
	 * 
	 * @param song - the song
	 * @return String
	 */
	public static String formatSongPrimaryMetadata(GDSong song) {
		if (song == null)
			return "Unknown song";
		return "__" + song.getSongTitle() + "__ by " + song.getSongAuthorName();
	}

	/**
	 * Formats song secondary metadata (ID + size + download link) to a human-readable format
	 * 
	 * @param song - the song
	 * @return String
	 */
	public static String formatSongSecondaryMetadata(GDSong song) {
		if (song == null)
			return ":warning: This level is using a song that doesn't exist at all on Newgrounds";
		return song.isCustom() ? ("SongID: " + song.getSongID() + " - Size: " + song.getSongSize() + "MB\n"
				+ Emojis.PLAY + " [Play on Newgrounds](https://www.newgrounds.com/audio/listen/" + song.getSongID() + ")  "
				+ Emojis.DOWNLOAD_SONG + " [Download MP3](" + song.getDownloadURL() + ")") : "Geometry Dash native audio track";
	}

	/**
	 * Returns the appropriate emoji for the difficulty of the given level
	 * 
	 * @param lp - the level
	 * @return String
	 */
	public static String difficultyToEmoji(GDLevelPreview lvl) {
		String difficulty = "icon_";

		if (lvl.isDemon())
			difficulty += "demon_" + lvl.getDemonDifficulty().toString();
		else if (lvl.isAuto())
			difficulty += "auto";
		else
			difficulty += lvl.getDifficulty().toString();
		if (lvl.isEpic())
			difficulty += "_epic";
		else if (lvl.getFeaturedScore() > 0)
			difficulty += "_featured";
		
		String output = Emojis.valueOf(difficulty.toUpperCase()).toString();
		
		if (lvl.getStars() > 0)
			output += Emojis.STAR + " x" + lvl.getStars();
		
		return output;
	}
	
	/**
	 * Gets the list of Discord accounts associated with the given GD account ID
	 * 
	 * @param gdAccountID - long
	 * @return List&lt;IUser&gt;
	 */
	public static List<IUser> getDiscordUsersLinkedToGDAccount(long gdAccountID) {
		return DatabaseUtils.query(UserSettings.class, "from UserSettings u where u.gdUserID = ?0", gdAccountID).stream()
				.map(x -> UltimateGDBot.client().fetchUser(x.getUserID()))
				.filter(x -> x != null)
				.collect(Collectors.toList());
	}
	
	/**
	 * Attempts to guess a GD user ID from the given string. If the string
	 * refers to a Discord user, this will look for a linked GD user. Returns -1
	 * if the user ID can't be guessed, returns -2 if GD servers are unavailable
	 * 
	 * @param str
	 *            - String
	 * @return long
	 */
	public static long guessGDUserIDFromString(String str) {
		long accountID = -1;
		try {
			long userID = BotUtils.extractIDFromMention(str);
			UserSettings us = DatabaseUtils.findByID(UserSettings.class, userID);
			
			if (us == null || !us.getLinkActivated())
				throw new NoSuchElementException();
			
			accountID = us.getGdUserID();
		} catch (IllegalArgumentException | NoSuchElementException e) {
			GDComponentList<GDUserPreview> results = (GDComponentList<GDUserPreview>) UltimateGDBot.cache()
					.readAndWriteIfNotExists("gd.usersearch." + str, () ->
							UltimateGDBot.gdClient().fetch(new GDUserSearchHttpRequest(str, 0)));
			
			if (results == null)
				accountID = -2;
			else if (!results.isEmpty())
				accountID = results.get(0).getAccountID();
		}
		
		return accountID;
	}

}
