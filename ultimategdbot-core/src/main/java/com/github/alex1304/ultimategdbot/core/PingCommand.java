package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;

import reactor.core.publisher.Mono;

@CommandSpec(
		aliases = "ping",
		shortDescription = "Pings the bot to check if it is alive."
)
class PingCommand {

	@CommandAction
	@CommandDoc("This command simply replies with 'Pong!'. If it replies successfully, congrats, "
			+ "the bot works for you!")
	public Mono<Void> run(Context ctx) {
		return ctx.reply("Pong! :ping_pong:").then();
	}
}
