package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="ping")
class PingCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return ctx.reply("Pong! :ping_pong:").then();
	}
}
