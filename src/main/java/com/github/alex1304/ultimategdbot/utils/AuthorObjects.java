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
	
	public static AuthorObject userProfile() {
		return new AuthorObject("User profile", "", "https://i.imgur.com/ppg4HqJ.png", "");
	}
	
	public static AuthorObject userPromoted() {
		return new AuthorObject("User promoted!", "", "https://i.imgur.com/zY61GDD.png", "");
	}
	
	public static AuthorObject userDemoted() {
		return new AuthorObject("User demoted...", "", "https://i.imgur.com/X53HV7d.png", "");
	}
}
