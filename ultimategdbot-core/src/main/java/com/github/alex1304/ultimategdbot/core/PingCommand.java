package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.CommandSpec;
import com.github.alex1304.ultimategdbot.api.command.Context;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="ping")
class PingCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return ctx.reply("Pong! :ping_pong:").then();
	}
}
