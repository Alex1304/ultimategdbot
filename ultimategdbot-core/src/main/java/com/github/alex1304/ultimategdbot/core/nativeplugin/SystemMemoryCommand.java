package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.SystemUnit;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SystemMemoryCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
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
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "View memory usage of the bot.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_MODERATOR;
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
