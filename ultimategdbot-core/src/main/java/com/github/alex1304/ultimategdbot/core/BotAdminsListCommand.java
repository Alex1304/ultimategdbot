package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

class BotAdminsListCommand implements Command {

	private final NativePlugin plugin;
	
	public BotAdminsListCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.getBot().getDatabase().query(BotAdmins.class, "from BotAdmins")
				.flatMap(admin -> ctx.getBot().getDiscordClients().next().flatMap(client -> client.getUserById(Snowflake.of(admin.getUserId()))))
				.map(BotUtils::formatDiscordUsername)
				.collectSortedList(String.CASE_INSENSITIVE_ORDER)
				.map(adminList -> {
					var sb = new StringBuilder("__**Bot administrator list:**__\n\n");
					adminList.forEach(admin -> sb.append(admin).append("\n"));
					if (adminList.isEmpty()) {
						sb.append("*(No data)*\n");
					}
					return sb.toString();
				}).flatMap(new PaginatedReplyMenuBuilder(this, ctx, true, false, 800)::build)
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("list");
	}

	@Override
	public String getDescription() {
		return "Lists users who are granted admin provileges on the bot.";
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
		return PermissionLevel.BOT_OWNER;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
