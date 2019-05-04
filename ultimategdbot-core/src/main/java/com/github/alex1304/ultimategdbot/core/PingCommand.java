package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;

import reactor.core.publisher.Mono;

class PingCommand implements Command {

	private final NativePlugin plugin;
	
	public PingCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.reply("Pong! :ping_pong:").then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("ping");
	}

	@Override
	public String getDescription() {
		return "Pings the bot to check if it is alive.";
	}

	@Override
	public String getLongDescription() {
		return "This command does not give the response time in milliseconds by default. However you can "
				+ "still achieve a similar behavior by doing `time ping`.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
