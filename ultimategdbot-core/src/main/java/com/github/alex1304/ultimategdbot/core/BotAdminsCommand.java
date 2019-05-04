package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;

import reactor.core.publisher.Mono;

class BotAdminsCommand implements Command {

	private final NativePlugin plugin;
	
	public BotAdminsCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return Mono.error(new InvalidSyntaxException(this));
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("botadmins");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of(new BotAdminsGrantCommand(plugin), new BotAdminsRevokeCommand(plugin), new BotAdminsListCommand(plugin));
	}

	@Override
	public String getDescription() {
		return "Manage users who have bot admin privileges.";
	}

	@Override
	public String getLongDescription() {
		return "Bot administrators have exclusive privileges on the bot. For example, they can use any command that normally "
				+ "requires server admin (like `setup`), and access to private commands such as `system memory`.\n"
				+ "Plugins may implement more commands exclusive to bot administrators.\n"
				+ "Use one of the available subcommands to grant, revoke, or list bot administrators.";
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
