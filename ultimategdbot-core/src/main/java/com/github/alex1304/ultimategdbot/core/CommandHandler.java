package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.plugin.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;

import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * UltimateGDbot's implementation of a command handler.
 * 
 * @author Alex1304
 *
 */
public class CommandHandler {

	private CommandHandler() {
	}

	public static void handleDefault(Mono<MessageCreateSpec> cmdExecMono, DiscordContext ctx) {
		cmdExecMono.doOnError(e -> {
			if (e instanceof CommandFailedException) {
				ctx.getEvent().getMessage().getChannel()
						.flatMap(c -> c.createMessage(":no_entry_sign: " + e.getMessage())).subscribe();
			} else {
				ctx.getEvent().getMessage().getChannel()
						.flatMap(c -> c.createMessage(":no_entry_sign: An internal error occured"))
						.subscribe();
				e.printStackTrace();
			}
		}).subscribe(mcs -> ctx.getEvent().getMessage().getChannel().flatMap(c -> c.createMessage(mcs)).subscribe());
	}

}