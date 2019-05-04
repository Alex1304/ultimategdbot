package com.github.alex1304.ultimategdbot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

class SequenceCommand implements Command {

	private final NativePlugin plugin;
	
	public SequenceCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		var cmdLines = new ArrayList<List<String>>();
		var lastI = 1;
		for (var i = 2 ; i < ctx.getArgs().size() ; i++) {
			if (getAliases().contains(ctx.getArgs().get(i))) {
				return Mono.error(new CommandFailedException("Cannot call `sequence` inside of a sequence."));
			}
			if (lastI != i && ctx.getArgs().get(i).equals(";")) {
				cmdLines.add(ctx.getArgs().subList(lastI, i));
				lastI = i + 1;
			}
		}
		if (lastI < ctx.getArgs().size()) {
			cmdLines.add(ctx.getArgs().subList(lastI, ctx.getArgs().size()));
		}
		if (cmdLines.size() > 10) {
			return Mono.error(new CommandFailedException("A sequence can contain at most 10 commands."));
		}
		var commands = cmdLines.stream()
				.map(ctx.getBot().getCommandKernel()::parseCommandLine)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());
		if (commands.size() < cmdLines.size()) {
			return Mono.error(new CommandFailedException("One of the specified commands does not exist."));
		}
		return Flux.fromIterable(commands)
				.concatMap(TupleUtils.function((command, args) -> ctx.getBot().getCommandKernel()
						.invokeCommand(command, new Context(ctx, args))
						.onErrorResume(e -> Mono.empty())))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("sequence");
	}

	@Override
	public String getDescription() {
		return "Allows to execute several commands at once sequentially.";
	}

	@Override
	public String getLongDescription() {
		return "Each command is separated by a semicolon. There must be a space before AND after the semicolon for it to be recognized properly.";
	}

	@Override
	public String getSyntax() {
		return "<command_1> ; <command_2> ; <command_3> ; ... ; <command_n>";
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
