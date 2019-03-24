package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SystemCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		return Mono.error(new InvalidSyntaxException(this));
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("system");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of(new SystemMemoryCommand(), new SystemExitCommand());
	}

	@Override
	public String getDescription() {
		return "Allows to perform operations on the bot on the system level.";
	}

	@Override
	public String getLongDescription() {
		return "It allows you to shutdown the bot with a custom exit code and view the current memory usage. See subcommands for more info.";
	}

	@Override
	public String getSyntax() {
		return "";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_ADMIN;
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
