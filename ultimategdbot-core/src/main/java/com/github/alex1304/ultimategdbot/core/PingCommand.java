package com.github.alex1304.ultimategdbot.core;

import static reactor.function.TupleUtils.function;

import java.time.Duration;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.util.BotUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Mono;

@CommandDescriptor(
		aliases = "ping",
		shortDescription = "Pings the bot to check if it is alive."
)
class PingCommand {
	
	private static final String PONG = "Pong! :ping_pong:";

	@CommandAction
	@CommandDoc("This command simply replies with 'Pong!' and gives latency information. If it replies "
			+ "successfully, congrats, the bot works for you!")
	public Mono<Void> run(Context ctx) {
		return ctx.reply(PONG)
				.elapsed()
				.flatMap(function((apiLatency, message) -> message.edit(
						spec -> spec.setContent(computeLatency(ctx.event(), apiLatency)))))
				.then();
	}
	
	private static String computeLatency(MessageCreateEvent event, long apiLatency) {
		return PONG + "\nDiscord API latency: " + BotUtils.formatDuration(Duration.ofMillis(apiLatency)) + "\n"
				+ "Discord Gateway latency: " + event.getClient()
						.getGatewayClient(event.getShardInfo().getIndex())
						.map(GatewayClient::getResponseTime)
						.map(BotUtils::formatDuration)
						.orElse("Unknown");
	}
}
