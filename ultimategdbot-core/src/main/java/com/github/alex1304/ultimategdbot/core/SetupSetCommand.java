package com.github.alex1304.ultimategdbot.core;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;

import discord4j.core.object.entity.Channel.Type;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SetupSetCommand implements Command {

	private final NativePlugin plugin;
	
	public SetupSetCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 3);
		var arg1 = ctx.getArgs().get(1);
		var arg2 = String.join(" ", ctx.getArgs().subList(2, ctx.getArgs().size()));
		var guildId = ctx.getEvent().getGuildId().map(Snowflake::asLong).orElse(0L);
		return Flux.fromIterable(ctx.getBot().getPlugins())
				.map(Plugin::getGuildConfigurationEntries)
				.filter(map -> map.containsKey(arg1))
				.switchIfEmpty(Mono.error(new CommandFailedException("There is no configuration entry with key `" + arg1 + "`.")))
				.next()
				.map(map -> map.get(arg1))
				.flatMap(entry -> ctx.getBot().getDatabase().performTransactionWhen(session -> entry.setFromString(session, arg2, guildId)))
				.onErrorMap(IllegalArgumentException.class, e -> new CommandFailedException("Cannot assign this value as `" + arg1 + "`: " + e.getMessage()))
				.then(ctx.reply(":white_check_mark: Settings updated!"))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("set");
	}

	@Override
	public String getDescription() {
		return "Assigns a new value to one of the guild configuration entries.";
	}

	@Override
	public String getLongDescription() {
		return "You can set `None` as value in order to reset the field.";
	}

	@Override
	public String getSyntax() {
		return "<key> <value>";
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
