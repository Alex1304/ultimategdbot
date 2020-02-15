package com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.util.DiscordParser;

import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;

public class GuildChannelConverter implements ParamConverter<GuildChannel> {

	@Override
	public Mono<GuildChannel> convert(Context ctx, String input) {
		return Mono.justOrEmpty(ctx.getEvent().getGuildId())
				.flatMap(guildId -> DiscordParser.parseGuildChannel(ctx.getBot(), guildId, input))
				.switchIfEmpty(Mono.error(new RuntimeException("Cannot find guild channels outside of a guild.")));
	}

	@Override
	public Class<GuildChannel> type() {
		return GuildChannel.class;
	}
}
