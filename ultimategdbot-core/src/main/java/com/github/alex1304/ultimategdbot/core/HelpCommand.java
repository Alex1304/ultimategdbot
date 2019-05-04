package com.github.alex1304.ultimategdbot.core;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;

class HelpCommand implements Command {

	private final NativePlugin plugin;
	
	public HelpCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		var rb = new PaginatedReplyMenuBuilder(this, ctx, true, false, Message.MAX_CONTENT_LENGTH - 10);
		if (ctx.getArgs().size() == 1) {
			var sb = new StringBuffer("Here is the list of commands you can use in this channel. Use `"
					+ ctx.getPrefixUsed() + "help <command>` to view the detailed documentation of a specific command.\n\n");
			return ctx.getEvent().getMessage().getChannel()
					.flatMap(c -> Flux.fromIterable(ctx.getBot().getPlugins())
							.sort(Comparator.comparing(Plugin::getName))
							.concatMap(plugin -> Flux.fromIterable(plugin.getProvidedCommands())
									.filter(cmd -> cmd.getChannelTypesAllowed().contains(c.getType()))
									.filterWhen(cmd -> cmd.getPermissionLevel().isGranted(ctx))
									.collectSortedList(Comparator.comparing(cmd -> BotUtils.joinAliases(cmd.getAliases())))
									.map(cmdList -> Tuples.of(plugin.getName(), cmdList)))
							.doOnNext(TupleUtils.consumer((pluginName, cmdList) -> {
								sb.append("**__").append(pluginName).append("__**\n");
								cmdList.forEach(cmd -> {
									sb.append('`');
									sb.append(ctx.getPrefixUsed());
									sb.append(BotUtils.joinAliases(cmd.getAliases()));
									sb.append("`: ");
									sb.append(cmd.getDescription());
									sb.append('\n');
								});
								sb.append('\n');
							}))
							.takeLast(1)
							.next())
					.flatMap(__ -> rb.build(sb.toString().stripTrailing())).then();
		}
		var cmdName = ArgUtils.concatArgs(ctx, 1);
		var cmd = ctx.getBot().getCommandKernel().parseCommandLine(cmdName);
		if (cmd.isEmpty() || cmd.get().getT2().size() > 1) {
			return Mono.error(new CommandFailedException("The command \"" + cmdName + "\" does not exist."));
		}
		return BotUtils.generateDefaultDocumentation(cmd.get().getT1(), ctx, cmdName)
				.flatMap(doc -> rb.build(doc)).then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("help", "manual");
	}

	@Override
	public String getDescription() {
		return "Displays the full list of commands, and provides a structured documentation for each of them.";
	}

	@Override
	public String getLongDescription() {
		return "You can also display help page for subcommands, by simply putting the subcommand name after the command name, for example `help setup set`.";
	}

	@Override
	public String getSyntax() {
		return "[<command_name> [<sub_commmand> ...]]";
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
