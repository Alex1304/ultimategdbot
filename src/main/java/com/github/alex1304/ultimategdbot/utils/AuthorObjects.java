package com.github.alex1304.ultimategdbot.utils;

import sx.blah.discord.api.internal.json.objects.EmbedObject.AuthorObject;

/**
 * Generic author objects for embeds used by the bot
 *
 * @author Alex1304
 */
public class AuthorObjects {

	public static AuthorObject awardedLevelAdded() {
		return new AuthorObject("New rated level!", "", "https://i.imgur.com/asoMj1W.png", "");
	}

	public static AuthorObject awardedLevelDeleted() {
		return new AuthorObject("Level un-rated...", "", "https://i.imgur.com/fPECXUz.png", "");
	}

	public static AuthorObject dailyLevel(long num) {
		return new AuthorObject("Daily level #" + num, "", "https://i.imgur.com/enpYuB8.png", "");
	}

	public static AuthorObject weeklyDemon(long num) {
		return new AuthorObject("Weekly demon #" + num, "", "https://i.imgur.com/kcsP5SN.png", "");
	}
	
	public static AuthorObject searchResult() {
		return new AuthorObject("Search result", "", "https://i.imgur.com/a9B6LyS.png", "");
	}

}
