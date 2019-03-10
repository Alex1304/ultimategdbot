package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.reply.PaginatedReplyMenuBuilder;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class HelpCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		return ctx.getEffectivePrefix().flatMap(prefix -> {
			var rb = new PaginatedReplyMenuBuilder(this, ctx, true, false);
			if (ctx.getArgs().size() == 1) {
				var sb = new StringBuffer("Here is the list of commands you can use in this channel:\n\n");
				return ctx.getEvent().getMessage().getChannel()
						.flatMap(c -> Flux.fromIterable(ctx.getBot().getCommandsFromPlugins().entrySet())
								.doOnNext(entry -> sb.append("**__").append(entry.getKey().getName()).append("__**\n"))
								.flatMap(entry -> Flux.fromIterable(entry.getValue())
										.filter(cmd -> cmd.getChannelTypesAllowed().contains(c.getType()))
										.filterWhen(cmd -> cmd.getPermissionLevel().isGranted(ctx))
										.doOnNext(cmd -> {
											sb.append('`');
											sb.append(prefix);
											sb.append(BotUtils.joinAliases(cmd.getAliases()));
											sb.append("`: ");
											sb.append(cmd.getDescription());
											sb.append('\n');
										}))
								.last())
						.flatMap(__ -> rb.build(sb.toString().stripTrailing()));
			}
			var cmdName = String.join(" ", ctx.getArgs().subList(1, ctx.getArgs().size()));
			var cmd = ctx.getBot().getCommandForName(cmdName);
			if (cmd == null) {
				return Mono.error(new CommandFailedException("The command \"" + cmdName + "\" does not exist."));
			}
			return rb.build(BotUtils.generateDefaultDocumentation(cmd, prefix, cmdName));
		}).then();
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
