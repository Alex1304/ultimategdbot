package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class DelayCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 3);
		var delay = ArgUtils.getArgAsInt(ctx, 1);
		if (delay < 1 || delay > 3600) {
			return Mono.error(new CommandFailedException("Delay must be a value between 1 and 3600."));
		}
		var parsedCmd = ctx.getBot().getCommandKernel().parseCommandLine(ctx.getArgs().subList(2, ctx.getArgs().size()));
		if (parsedCmd.isEmpty()) {
			return Mono.error(new CommandFailedException("Command not found."));
		}
		var cmd = parsedCmd.get().getT1();
		var args = parsedCmd.get().getT2();
		var duration = Duration.ofSeconds(delay);
		var cmdLine = "`" + ctx.getPrefixUsed() + ArgUtils.concatArgs(ctx, 2) + "`";
		return ctx.reply(":thumbsup: Will execute " + cmdLine + " in "
				+ BotUtils.formatTimeMillis(duration) + ".")
				.then(Mono.delay(duration))
				.then(ctx.reply(ctx.getEvent().getMessage().getAuthor().get().getMention() + ", now executing " + cmdLine + "..."))
				.then(ctx.getBot().getCommandKernel().invokeCommand(cmd, ctx.fork(args)))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("delay");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "Delays the execution of a command up to 1 hour.";
	}

	@Override
	public String getLongDescription() {
		return "This can be useful in order to schedule tasks or make fun command chains.\n"
				+ "The delay must not exceed 1 hour. Note that if the delay is long, it is not guaranteed "
				+ "that it will be executed (for example if the bot restarts before the delay ends).";
	}

	@Override
	public String getSyntax() {
		return "<nb_seconds> <command>";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.PUBLIC;
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
