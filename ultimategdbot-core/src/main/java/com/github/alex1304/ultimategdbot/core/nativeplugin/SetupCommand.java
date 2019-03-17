package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SetupCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		var rb = new PaginatedReplyMenuBuilder(this, ctx, true, false);
		var sb = new StringBuffer();
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
		return Set.of(new SetupSetCommand());
	}

	@Override
	public String getDescription() {
		return "View and edit bot setup for this guild.";
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
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Collections.emptyMap();
	}
}
