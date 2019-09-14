package com.github.alex1304.ultimategdbot.api.command.parser;

import com.github.alex1304.ultimategdbot.api.command.ArgumentParseException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.utils.DiscordParser;

import discord4j.core.object.entity.GuildChannel;
import reactor.core.publisher.Mono;

public class GuildChannelParser implements Parser<GuildChannel> {

	@Override
	public Mono<GuildChannel> parse(Context ctx, String input) {
		return Mono.justOrEmpty(ctx.getEvent().getGuildId())
				.flatMap(guildId -> DiscordParser.parseGuildChannel(ctx.getBot(), guildId, input))
				.onErrorMap(e -> new ArgumentParseException(e.getMessage()))
				.switchIfEmpty(Mono.error(new ArgumentParseException("Cannot find guild channels outside of a guild.")));
	}

	@Override
	public Class<GuildChannel> type() {
		return GuildChannel.class;
	}
}
