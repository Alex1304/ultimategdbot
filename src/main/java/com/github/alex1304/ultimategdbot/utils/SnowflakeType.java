package com.github.alex1304.ultimategdbot.utils;

import java.util.function.Function;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

import sx.blah.discord.handle.obj.IIDLinkedObject;

/**
 * Enumerates the type of snowflakes and each item describes how to resolve them
 *
 * @author Alex1304
 */
public enum SnowflakeType {
	GUILD(l -> UltimateGDBot.client().getGuildByID(l)),
	CHANNEL(l -> UltimateGDBot.client().getChannelByID(l)),
	ROLE(l -> UltimateGDBot.client().getRoleByID(l)),
	USER(l -> UltimateGDBot.client().fetchUser(l));
	
	private Function<Long, IIDLinkedObject> func;
	
	private SnowflakeType(Function<Long, IIDLinkedObject> func) {
		this.func = func;
	}

	/**
	 * Gets the func
	 *
	 * @return Function<Long,IIDLinkedObject>
	 */
	public Function<Long, IIDLinkedObject> getFunc() {
		return func;
	}
}
