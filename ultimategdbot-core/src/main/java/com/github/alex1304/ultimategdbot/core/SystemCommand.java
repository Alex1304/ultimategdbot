package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;
import com.github.alex1304.ultimategdbot.api.command.annotation.Subcommand;
import com.github.alex1304.ultimategdbot.api.command.parser.IntParser;
import com.github.alex1304.ultimategdbot.api.utils.SystemUnit;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="system", permLevel=PermissionLevel.BOT_ADMIN)
class SystemCommand {
	
	@Subcommand("memory")
	@CommandAction
	public Mono<Void> runMemory(Context ctx) {
		System.gc();
		var total = Runtime.getRuntime().totalMemory();
		var free = Runtime.getRuntime().freeMemory();
		var max = Runtime.getRuntime().maxMemory();
		var sb = new StringBuilder();
		sb.append("**Maximum system RAM available:** " + SystemUnit.format(max) + "\n");
		sb.append("**Current JVM size:** " + SystemUnit.format(total)
				+ " (" + String.format("%.2f", total * 100 / (double) max) + "%)\n");
		sb.append("**Memory effectively used by the bot:** " + SystemUnit.format(total - free)
				+ " (" + String.format("%.2f", (total - free) * 100 / (double) max) + "%)\n");
		return ctx.reply(sb.toString()).then();
	}
	
	@Subcommand("exit")
	@CommandAction(IntParser.class)
	public Mono<Void> runExit(Context ctx, int code) {
		if (code < 0 || code > 255) {
			return Mono.error(new CommandFailedException("Exit code must be between 0 and 255. "
					+ "If you don't know which code to use, 0 is preferred."));
		}
		var message = "Terminating JVM with exit code " + code + "...";
		return ctx.reply(message)
				.then(ctx.getBot().log(":warning: " + message))
				.doAfterTerminate(() -> System.exit(code))
				.then();
	}
}
