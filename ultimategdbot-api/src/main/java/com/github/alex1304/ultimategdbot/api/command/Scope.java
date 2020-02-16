package com.github.alex1304.ultimategdbot.api.command;

import java.util.function.Predicate;

import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;

/**
 * The scope of a command, that is, the kind of channels where the command is applicable.
 */
public enum Scope {
	ANYWHERE(c -> true),
	DM_ONLY(c -> c instanceof PrivateChannel),
	GUILD_ONLY(c -> c instanceof GuildMessageChannel);
	
	private Predicate<Channel> isInScope;
	
	private Scope(Predicate<Channel> isInScope) {
		this.isInScope = isInScope;
	}
	
	public boolean isInScope(Channel channel) {
		return isInScope.test(channel);
	}
}