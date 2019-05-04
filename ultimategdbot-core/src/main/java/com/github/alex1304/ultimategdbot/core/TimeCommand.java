package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

class TimeCommand implements Command {

	private final NativePlugin plugin;
	
	public TimeCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

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
		return ctx.getBot().getCommandKernel().invokeCommand(cmd, new Context(ctx, args))
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
	public Plugin getPlugin() {
		return plugin;
	}
}
