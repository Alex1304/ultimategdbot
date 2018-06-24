package com.github.alex1304.ultimategdbot.core;

/**
 * Entry point of the program
 *
 * @author Alex1304
 */
public class Main {
	
	public static void main(String[] args) throws Exception {
		Database.init();
		UltimateGDBot.init();
		UltimateGDBot.client().getDispatcher().registerListener(new DiscordEvents());
		UltimateGDBot.client().login();
	}
}
