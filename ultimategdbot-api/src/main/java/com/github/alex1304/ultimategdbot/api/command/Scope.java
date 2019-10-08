package com.github.alex1304.ultimategdbot.api.command;

import java.util.EnumSet;
import java.util.function.Predicate;

import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Channel.Type;

/**
 * The scope of a command, that is, the kind of channels where the command is applicable.
 */
public enum Scope {
	ANYWHERE(c -> true),
	DM_ONLY(c -> c.getType() == Type.DM),
	GUILD_ONLY(c -> EnumSet.of(Type.GUILD_TEXT, Type.GUILD_NEWS, Type.GUILD_STORE).contains(c.getType()));
	
	private Predicate<Channel> isInScope;
	
	private Scope(Predicate<Channel> isInScope) {
		this.isInScope = isInScope;
	}
	
	public boolean isInScope(Channel channel) {
		return isInScope.test(channel);
	}
}