package com.github.alex1304.ultimategdbot.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

@CommandSpec(aliases="burstmessages", permLevel=PermissionLevel.BOT_OWNER)
class BurstMessagesCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(BurstMessagesCommand.class);

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return ctx.getEvent().getGuild()
				.flatMapMany(Guild::getChannels)
				.ofType(TextChannel.class)
				.filter(channel -> channel.getName().startsWith("test"))
				.collectList()
				.doOnNext(channelList -> Flux.fromIterable(channelList)
						.flatMap(channel -> Flux.range(1, 5)
								.map(String::valueOf)
								.flatMap(channel::createMessage))
						.count()
						.elapsed()
						.doOnNext(TupleUtils.consumer(
								(time, count) -> LOGGER.info("Sent {} messages in {} milliseconds ({} messages/s)",
										count, time, (count / (double) time) * 1000)))
						.subscribe()) // Separate subscribe in order to improve accuracy of elapsed time
				.then();
	}
}