package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import reactor.core.publisher.Mono;

class DelayCommand implements Command {

	private final NativePlugin plugin;
	
	public DelayCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

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
		if (cmd instanceof DelayCommand) {
			return Mono.error(new CommandFailedException("The `" + args.get(0) + "` command cannot be delayed."));
		}
		var duration = Duration.ofSeconds(delay);
		var cmdLine = "`" + ctx.getPrefixUsed() + ArgUtils.concatArgs(ctx, 2) + "`";
		return ctx.reply(":thumbsup: Will execute " + cmdLine + " in "
				+ BotUtils.formatTimeMillis(duration) + ".")
				.then(Mono.delay(duration))
				.then(ctx.reply(ctx.getEvent().getMessage().getAuthor().get().getMention() + ", now executing " + cmdLine + "..."))
				.then(ctx.getBot().getCommandKernel().invokeCommand(cmd, new Context(ctx, args))
						.onErrorResume(e -> Mono.empty()))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("delay");
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
	public Plugin getPlugin() {
		return plugin;
	}
}
