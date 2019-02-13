package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SetupSetCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		if (ctx.getArgs().size() < 3) {
			return Mono.error(new InvalidSyntaxException(this));
		}
		var arg1 = ctx.getArgs().get(1);
		var arg2 = String.join(" ", ctx.getArgs().subList(2, ctx.getArgs().size()));
		try {
			ctx.setGuildSetting(arg1, arg2);
		} catch (NoSuchElementException e) {
			return Mono.error(new CommandFailedException("There is no settings entry with key `" + arg1 + "`."));
		} catch (IllegalArgumentException e) {
			return Mono.error(new CommandFailedException("Cannot assign this value to key `" + arg1 + "`: " + e.getMessage()));
		}
		return ctx.reply(":white_check_mark: Settings updated!").then();
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
