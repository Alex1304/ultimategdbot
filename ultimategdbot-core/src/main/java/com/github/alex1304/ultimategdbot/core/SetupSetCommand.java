package com.github.alex1304.ultimategdbot.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SetupSetCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 3);
		var arg1 = ctx.getArgs().get(1);
		var arg2 = String.join(" ", ctx.getArgs().subList(2, ctx.getArgs().size()));
		return ctx.setGuildSetting(arg1, arg2)
				.onErrorMap(NoSuchElementException.class, e -> new CommandFailedException("There is no configuration entry with key `" + arg1 + "`."))
				.onErrorMap(IllegalArgumentException.class, e -> new CommandFailedException("Cannot assign this value as `" + arg1 + "`: " + e.getMessage()))
				.then(ctx.reply(":white_check_mark: Settings updated!"))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("set");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Collections.emptySet();
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
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Collections.emptyMap();
	}

}
