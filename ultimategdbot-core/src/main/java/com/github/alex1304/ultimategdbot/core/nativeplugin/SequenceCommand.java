package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SequenceCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		var cmdLines = new ArrayList<List<String>>();
		var lastI = 1;
		for (var i = 2 ; i < ctx.getArgs().size() ; i++) {
			if (lastI != i && ctx.getArgs().get(i).equals(";")) {
				cmdLines.add(ctx.getArgs().subList(lastI, i));
				lastI = i + 1;
			}
		}
		if (lastI < ctx.getArgs().size()) {
			cmdLines.add(ctx.getArgs().subList(lastI, ctx.getArgs().size()));
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
				.concatMap(cmdTuple -> ctx.getBot().getCommandKernel().invokeCommand(cmdTuple.getT1(), ctx.fork(cmdTuple.getT2()))
						.onErrorResume(e -> Mono.empty()))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("sequence");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of();
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
