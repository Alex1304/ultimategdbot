package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;

import reactor.core.publisher.Mono;

class BlacklistCommand implements Command {

	private final NativePlugin plugin;
	
	public BlacklistCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return Mono.error(new InvalidSyntaxException(this));
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("blacklist");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of(new BlacklistAddCommand(plugin), new BlacklistRemoveCommand(plugin));
	}

	@Override
	public String getDescription() {
		return "Restrict guilds, channels or users from using the bot.";
	}

	@Override
	public String getLongDescription() {
		return "In case of abuse, you may use this command to disable the bot for a specific user, "
				+ "a specific channel or a whole Discord server.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_OWNER;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
