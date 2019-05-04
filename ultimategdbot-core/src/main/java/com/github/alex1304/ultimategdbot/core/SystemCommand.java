package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;

import reactor.core.publisher.Mono;

class SystemCommand implements Command {

	private final NativePlugin plugin;
	
	public SystemCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return Mono.error(new InvalidSyntaxException(this));
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("system");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of(new SystemMemoryCommand(plugin), new SystemExitCommand(plugin));
	}

	@Override
	public String getDescription() {
		return "Allows to perform operations on the bot on the system level.";
	}

	@Override
	public String getLongDescription() {
		return "It allows you to shutdown the bot with a custom exit code and view the current memory usage. See subcommands for more info.";
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
