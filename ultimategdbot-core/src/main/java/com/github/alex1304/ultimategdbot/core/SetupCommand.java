package com.github.alex1304.ultimategdbot.core;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

class SetupCommand implements Command {

	private final NativePlugin plugin;
	
	public SetupCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		var rb = new PaginatedReplyMenuBuilder(this, ctx, true, false, Message.MAX_CONTENT_LENGTH - 10);
		var sb = new StringBuffer("Here you can configure the bot for this server. "
				+ "You can update a field by doing `" + ctx.getPrefixUsed() + "setup set <field> <value>`. "
				+ "Use `None` as value to reset a field.\n\n");
		return ctx.getGuildSettings().doOnNext(map -> map.forEach((plugin, entries) -> {
			sb.append("**__").append(plugin.getName()).append("__**\n");
			if (entries.isEmpty()) {
				sb.append("_(Nothing to configure here)_\n");
				return;
			}
			entries.forEach((k, v) -> {
				sb.append('`');
				sb.append(k);
				sb.append("`: ");
				sb.append(v);
				sb.append('\n');
			});
			sb.append('\n');
		})).flatMap(__ -> rb.build(sb.toString().stripTrailing())).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("setup", "settings", "configure", "config");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of(new SetupSetCommand(plugin));
	}

	@Override
	public String getDescription() {
		return "View and edit bot setup for this guild.";
	}

	@Override
	public String getLongDescription() {
		return "Running this command without arguments displays the current setup. Use the `set` subcommand to eidt a value.\n"
				+ "By default you can only configure the prefix and the server mod role, but more configuration entries may be added by plugins.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.SERVER_ADMIN;
	}

	@Override
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT);
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
