package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.SystemUnit;

import reactor.core.publisher.Mono;

class SystemMemoryCommand implements Command {

	private final NativePlugin plugin;
	
	public SystemMemoryCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
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

	@Override
	public Set<String> getAliases() {
		return Set.of("memory");
	}

	@Override
	public String getDescription() {
		return "View memory usage of the bot.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_ADMIN;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
