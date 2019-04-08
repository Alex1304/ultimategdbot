package com.github.alex1304.ultimategdbot.core;

import java.util.Collections;
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
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

class HelpCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		var rb = new PaginatedReplyMenuBuilder(this, ctx, true, false, Message.MAX_CONTENT_LENGTH - 10);
		if (ctx.getArgs().size() == 1) {
			var sb = new StringBuffer("Here is the list of commands you can use in this channel. Use `"
					+ ctx.getPrefixUsed() + "help <command>` to view the detailed documentation of a specific command.\n\n");
			return ctx.getEvent().getMessage().getChannel()
					.flatMap(c -> Flux.fromIterable(ctx.getBot().getCommandKernel().getCommandsGroupedByPlugins().entrySet())
							.flatMapSequential(entry -> Flux.fromIterable(entry.getValue())
										.filter(cmd -> cmd.getChannelTypesAllowed().contains(c.getType()))
										.filterWhen(cmd -> cmd.getPermissionLevel().isGranted(ctx))
										.collectList()
										.map(cmdList -> Tuples.of(entry.getKey(), cmdList)))
							.doOnNext(tuple -> {
								sb.append("**__").append(tuple.getT1()).append("__**\n");
								tuple.getT2().forEach(cmd -> {
									sb.append('`');
									sb.append(ctx.getPrefixUsed());
									sb.append(BotUtils.joinAliases(cmd.getAliases()));
									sb.append("`: ");
									sb.append(cmd.getDescription());
									sb.append('\n');
								});
								sb.append('\n');
							})
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
	public Set<Command> getSubcommands() {
		return Collections.emptySet();
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
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.PUBLIC;
	}

	@Override
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT, Type.DM);
	}

	@Override
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Collections.emptyMap();
	}
}
