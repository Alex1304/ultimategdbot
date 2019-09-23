package com.github.alex1304.ultimategdbot.core;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.Command;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;
import com.github.alex1304.ultimategdbot.api.utils.Markdown;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;

@CommandSpec(
		aliases = { "help", "manual" },
		shortDescription = "Provides documentation for all commands."
)
class HelpCommand {
	
	@CommandAction
	@CommandDoc("If used without arguments, this command will display the list of all commands that you can use in the current channel.\n"
			+ "If `command` is specified, it will display information on the command, such as its syntax, the flags available, and a "
			+ "detailed description of what it does. Each command may have one or several subcommands, each of them with their own "
			+ "documentation. For such commands, you can specify which subcommand you want to get info on via the second `subcommand` "
			+ "argument.")
	public Mono<Void> run(Context ctx, @Nullable String command, @Nullable String subcommand) {
		return command == null ? displayCommandList(ctx) : displayCommandDocumentation(ctx, command.toLowerCase(), subcommand);
	}

	private Mono<Void> displayCommandList(Context ctx) {
		var sb = new StringBuilder();
		return ctx.getEvent().getMessage().getChannel()
				.flatMap(channel -> Flux.fromIterable(ctx.getBot().getPlugins())
						.sort(Comparator.comparing(Plugin::getName))
						.concatMap(plugin -> Flux.fromIterable(plugin.getCommandProvider().getProvidedCommands())
								.filter(cmd -> cmd.getScope().isInScope(channel))
								.filterWhen(cmd -> cmd.getPermissionLevel().isGranted(ctx))
								.collectSortedList(Comparator.comparing(HelpCommand::joinAliases))
								.map(cmdList -> Tuples.of(plugin.getName(), cmdList)))
						.doOnNext(TupleUtils.consumer((pluginName, cmdList) -> {
							sb.append(Markdown.bold(Markdown.underline(pluginName))).append("\n");
							cmdList.forEach(cmd -> {
								sb.append(Markdown.code(ctx.getPrefixUsed() + joinAliases(cmd)));
								sb.append(" - ");
								sb.append(cmd.getDocumentation().getShortDescription());
								sb.append('\n');
							});
							sb.append('\n');
						})).then())
				.then(Mono.defer(() -> BotUtils.sendPaginatedMessage(ctx, sb.toString(), Message.MAX_CONTENT_LENGTH)));
	}
	
	private Mono<Void> displayCommandDocumentation(Context ctx, String commandName, String subcommand) {
		var selectedSubcommand = subcommand == null ? "" : subcommand.toLowerCase();
		var command = new AtomicReference<Command>();
		return Mono.justOrEmpty(Optional.ofNullable(ctx.getBot().getCommandKernel().getCommandByAlias(commandName)))
				.switchIfEmpty(Mono.error(new CommandFailedException("Command " + Markdown.code(commandName) + " not found.")))
				.doOnNext(command::set)
				.filter(cmd -> cmd.getDocumentation().getEntries().containsKey(selectedSubcommand))
				.switchIfEmpty(Mono.error(() -> {
					var subcommands = command.get().getDocumentation().getEntries().keySet().stream()
							.map(subcmd -> Markdown.code(ctx.getPrefixUsed() + "help " + commandName + (subcmd.isEmpty() ? "" : " " + subcmd)))
							.collect(Collectors.joining("\n"));
					if (selectedSubcommand.isEmpty()) {
						return new CommandFailedException("Nothing found for the command " + Markdown.code(commandName) + " alone.\n"
								+ "Try one of the available subcommands:\n" + subcommands);
					}
					return new CommandFailedException("Subcommand " + Markdown.code(selectedSubcommand) + " for command " + Markdown.code(commandName) + " not found.\n"
							+ "Available subcommands:\n" + subcommands);
				}))
				.map(cmd -> formatDoc(cmd, ctx.getPrefixUsed(), commandName, selectedSubcommand))
				.flatMap(doc -> BotUtils.sendPaginatedMessage(ctx, doc, Message.MAX_CONTENT_LENGTH));
	}
	
	private static String joinAliases(Command cmd) {
		return cmd.getAliases().stream()
				.sorted((a, b) -> a.length() - b.length() == 0 ? a.compareTo(b) : a.length() - b.length())
				.collect(Collectors.joining("|"));
	}
	
	private static String formatDoc(Command cmd, String prefix, String selectedCommand, String selectedSubcommand) {
		var entry = cmd.getDocumentation().getEntries().get(selectedSubcommand);
		var sb = new StringBuilder(Markdown.code(prefix + selectedCommand))
				.append(" - ")
				.append(cmd.getDocumentation().getShortDescription())
				.append(selectedSubcommand.isEmpty() ? "" : "\nSubcommand: " + Markdown.code(selectedSubcommand))
				.append("\n\n")
				.append(Markdown.bold(Markdown.underline("Syntax")))
				.append("\n")
				.append(Markdown.codeBlock(prefix + joinAliases(cmd) + (selectedSubcommand.isEmpty() ? "" : " " + selectedSubcommand) + " " + entry.getSyntax()))
				.append(entry.getDescription())
				.append("\n");
		if (!entry.getFlagInfo().isEmpty()) {
			sb.append("\n").append(Markdown.bold(Markdown.underline("Flags"))).append("\n");
			entry.getFlagInfo().forEach((name, info) -> {
				sb.append(Markdown.code("--" + name + (info.getValueFormat().isBlank() ? "" : "=<" + info.getValueFormat() + ">")))
						.append(": ")
						.append(info.getDescription())
						.append("\n");
			});
		}
		if (cmd.getDocumentation().getEntries().size() > 1) {
			sb.append("\n").append(Markdown.bold(Markdown.underline("See Also"))).append("\n");
			cmd.getDocumentation().getEntries().forEach((otherPage, otherEntry) -> {
				if (otherPage.equals(selectedSubcommand)) {
					return;
				}
				sb.append(Markdown.code(prefix + "help " + selectedCommand + (otherPage.isEmpty() ? "" : " " + otherPage)))
						.append(": ")
						.append(extractFirstSentence(otherEntry.getDescription()))
						.append("\n");
			});
		}
		return sb.toString();
	}
	
	private static String extractFirstSentence(String text) {
		var parts = text.split("\\.", 2);
		return (parts.length == 0 ? "" : parts[0]) + ".";
	}
}
