package com.github.alex1304.ultimategdbot.core;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.Command;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.Markdown;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;

@CommandSpec(aliases = { "help", "manual" },
		shortDescription = "Provides documentation for all commands.")
class HelpCommand {
	
	@CommandAction
	public Mono<Void> execute(Context ctx, @Nullable String command, @Nullable String subcommand) {
		return command == null ? displayCommandList(ctx) : displayCommandDocumentation(ctx, command, subcommand);
	}

	private Mono<Void> displayCommandList(Context ctx) {
		var sb = new StringBuilder();
		return ctx.getEvent().getMessage().getChannel()
				.flatMap(channel -> Flux.fromIterable(ctx.getBot().getPlugins())
						.sort(Comparator.comparing(Plugin::getName))
						.concatMap(plugin -> Flux.fromIterable(plugin.getCommandProvider().getProvidedCommands())
								.filter(cmd -> cmd.getScope().isInScope(channel))
								.filterWhen(cmd -> cmd.getPermissionLevel().isGranted(ctx))
								.collectSortedList(Comparator.comparing(this::joinAliases))
								.map(cmdList -> Tuples.of(plugin.getName(), cmdList)))
						.doOnNext(TupleUtils.consumer((pluginName, cmdList) -> {
							sb.append(Markdown.bold(Markdown.underline(pluginName))).append("\n");
							cmdList.forEach(cmd -> {
								sb.append(Markdown.code(ctx.getPrefixUsed() + joinAliases(cmd)));
								sb.append(": ");
								sb.append(cmd.getDocumentation().getShortDescription());
								sb.append('\n');
							});
							sb.append('\n');
						})).then())
				.then(Mono.defer(() -> BotUtils.sendPaginatedMessage(ctx, sb.toString(), Message.MAX_CONTENT_LENGTH)));
	}
	
	private Mono<Void> displayCommandDocumentation(Context ctx, String command, String subcommand) {
		return Mono.empty();
	}
	
	private String joinAliases(Command cmd) {
		return cmd.getAliases().stream()
				.sorted((a, b) -> a.length() - b.length() == 0 ? a.compareTo(b) : a.length() - b.length())
				.collect(Collectors.joining("|"));
	}
}
