package com.github.alex1304.ultimategdbot.core;

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
import reactor.util.function.Tuple2;

class TimeCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		var parsedCmd = ctx.getBot().getCommandKernel().parseCommandLine(ctx.getArgs().subList(1, ctx.getArgs().size()));
		if (parsedCmd.isEmpty()) {
			return Mono.error(new CommandFailedException("Command not found."));
		}
		var cmd = parsedCmd.get().getT1();
		var args = parsedCmd.get().getT2();
		if (cmd instanceof TimeCommand || cmd instanceof DelayCommand) {
			return Mono.error(new CommandFailedException("The `" + args.get(0) + "` command cannot be timed."));
		}
		return ctx.getBot().getCommandKernel().invokeCommand(cmd, ctx.fork(args))
				.onErrorResume(e -> Mono.empty())
				.then(Mono.just(0))
				.elapsed()
				.map(Tuple2::getT1)
				.flatMap(time -> ctx.reply(":timer: Execution time: **" + BotUtils.formatTimeMillis(Duration.ofMillis(time)) + "**."))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("time");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "Measures the execution time of a command.";
	}

	@Override
	public String getLongDescription() {
		return "Put the desired command with its arguments as argument of the `time` command. Note that it doesn't support measuring time of interactive menus.";
	}

	@Override
	public String getSyntax() {
		return "<command>";
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
