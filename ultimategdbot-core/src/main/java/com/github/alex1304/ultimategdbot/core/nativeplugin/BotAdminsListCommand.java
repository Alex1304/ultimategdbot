package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class BotAdminsListCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.getBot().getDatabase().query(BotAdmins.class, "from BotAdmins")
				.flatMap(admin -> ctx.getBot().getDiscordClients().flatMap(client -> client.getUserById(Snowflake.of(admin.getUserId()))).next())
				.onErrorResume(e -> Mono.empty())
				.map(BotUtils::formatDiscordUsername)
				.collectList()
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
	public Set<Command> getSubcommands() {
		return Set.of();
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
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT, Type.DM);
	}

	@Override
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Map.of();
	}
}
