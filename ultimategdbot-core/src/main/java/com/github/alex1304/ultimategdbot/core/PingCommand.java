package com.github.alex1304.ultimategdbot.core;

import static discord4j.core.retriever.EntityRetrievalStrategy.REST;
import static discord4j.core.retriever.EntityRetrievalStrategy.STORE_FALLBACK_REST;

import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;

import discord4j.core.object.entity.Role;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

@CommandDescriptor(
		aliases = "ping",
		shortDescription = "Pings the bot to check if it is alive."
)
class PingCommand {

	@CommandAction
	@CommandDoc("This command simply replies with 'Pong!'. If it replies successfully, congrats, "
			+ "the bot works for you!")
	public Mono<Void> run(Context ctx) {
		return ctx.getBot().getGateway()
		        .withRetrievalStrategy(STORE_FALLBACK_REST)
		        .getGuildById(Snowflake.of(361255823357509645L))
		        .flatMapMany(guild -> guild.getRoles(REST))
		        .collectList()
		        .flatMap(roleList -> ctx.reply(roleList.stream().map(Role::getName).collect(Collectors.joining("\n"))))
		        .then();
	}
}
