package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class SystemExitCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		if (ctx.getArgs().size() == 1) {
			return Mono.error(new InvalidSyntaxException(this));
		}
		try {
			var code = Integer.parseInt(ctx.getArgs().get(1));
			if (code < 0 || code > 255) {
				return Mono.error(new CommandFailedException("Exit code must be between 0 and 255. If you don't know which code to use, 0 is preferred."));
			}
			var message = "Terminating JVM with exit code " + code + "...";
			return ctx.reply(message)
					.then(ctx.getBot().log(":warning: " + message))
					.doAfterTerminate(() -> System.exit(code))
					.then();
		} catch (NumberFormatException e) {
			return Mono.error(new CommandFailedException("Invalid exit code."));
		}
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("exit");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "Allows to shut down the bot.";
	}

	@Override
	public String getSyntax() {
		return "<code>";
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