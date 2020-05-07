package com.github.alex1304.ultimategdbot.core;

import static com.github.alex1304.ultimategdbot.api.util.BotUtils.sendPaginatedMessage;
import static com.github.alex1304.ultimategdbot.api.util.Markdown.bold;
import static com.github.alex1304.ultimategdbot.api.util.Markdown.code;
import static com.github.alex1304.ultimategdbot.api.util.Markdown.codeBlock;
import static com.github.alex1304.ultimategdbot.api.util.Markdown.underline;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static reactor.function.TupleUtils.consumer;
import static reactor.function.TupleUtils.function;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.Command;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.util.menu.PaginationControls;

import discord4j.core.object.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;

@CommandDescriptor(
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

	private static Mono<Void> displayCommandList(Context ctx) {
		var sb = new StringBuilder("Here is the list of commands you can use in this channel. "
				+ "Use " + code(ctx.prefixUsed() + "help <command>") + " to view the detailed documentation of a specific command.\n\n");
		return ctx.event().getMessage().getChannel()
				.flatMap(channel -> Flux.fromIterable(ctx.bot().plugins())
						.sort(comparing(Plugin::getName))
						.concatMap(plugin -> Flux.fromIterable(plugin.getCommandProvider().getProvidedCommands())
								.filter(cmd -> cmd.getScope().isInScope(channel))
								.filterWhen(cmd -> ctx.bot().commandKernel().getPermissionChecker().isGranted(cmd.getRequiredPermission(), ctx))
								.filterWhen(cmd -> ctx.bot().commandKernel().getPermissionChecker().isGranted(cmd.getMinimumPermissionLevel(), ctx))
								.collectSortedList(comparing(HelpCommand::joinAliases))
								.map(cmdList -> Tuples.of(plugin.getName(), cmdList)))
						.doOnNext(consumer((pluginName, cmdList) -> {
							sb.append(bold(underline(pluginName))).append("\n");
							cmdList.stream()
									.filter(cmd -> !cmd.getDocumentation().isHidden())
									.forEach(cmd -> {
										sb.append(code(ctx.prefixUsed() + joinAliases(cmd)));
										sb.append(" - ");
										sb.append(cmd.getDocumentation().getShortDescription());
										sb.append('\n');
									});
							sb.append('\n');
						})).then())
				.then(Mono.defer(() -> sendPaginatedMessage(ctx, sb.toString(), PaginationControls.getDefault(), Message.MAX_CONTENT_LENGTH)));
	}
	
	private static Mono<Void> displayCommandDocumentation(Context ctx, String commandName, String subcommand) {
		var selectedSubcommand = subcommand == null ? "" : subcommand.toLowerCase();
		var command = new AtomicReference<Command>();
		return Mono.justOrEmpty(ctx.bot().commandKernel().getCommandByAlias(commandName))
				.switchIfEmpty(Mono.error(new CommandFailedException("Command " + code(commandName) + " not found.")))
				.doOnNext(command::set)
				.flatMap(cmd -> findAvailableSubcommands(cmd, ctx).collectList().map(subcommands -> Tuples.of(subcommands, cmd)))
				.flatMap(function((subcommands, cmd) -> {
					var formattedSubcommands = subcommands.stream()
							.map(subcmd -> code(ctx.prefixUsed() + "help " + commandName + (subcmd.isEmpty() ? "" : " " + subcmd)))
							.collect(joining("\n"));
					if (subcommands.contains(selectedSubcommand)) {
						return Mono.just(cmd);
					}
					if (!selectedSubcommand.isEmpty()) {
						return Mono.error(new CommandFailedException("Subcommand " + code(selectedSubcommand) + " for command " + code(commandName) + " not found.\n"
								+ "Available subcommands:\n" + formattedSubcommands));
					}
					return Mono.error(new CommandFailedException("Nothing found for the command " + code(commandName) + " alone.\n"
								+ "Try one of the available subcommands:\n" + formattedSubcommands));
				}))
				.map(cmd -> formatDoc(cmd, ctx.prefixUsed(), ctx.bot().config().getFlagPrefix(), commandName, selectedSubcommand))
				.flatMap(doc -> sendPaginatedMessage(ctx, doc));
	}
	
	private static Flux<String> findAvailableSubcommands(Command cmd, Context ctx) {
		return Flux.fromIterable(cmd.getDocumentation().getEntries().keySet());
	}
	
	private static String joinAliases(Command cmd) {
		return cmd.getAliases().stream()
				.sorted((a, b) -> a.length() - b.length() == 0 ? a.compareTo(b) : a.length() - b.length())
				.collect(Collectors.joining("|"));
	}
	
	private static String formatDoc(Command cmd, String prefix, String flagPrefix, String selectedCommand, String selectedSubcommand) {
		var entry = cmd.getDocumentation().getEntries().get(selectedSubcommand);
		var sb = new StringBuilder(code(prefix + selectedCommand))
				.append(" - ")
				.append(cmd.getDocumentation().getShortDescription())
				.append(selectedSubcommand.isEmpty() ? "" : "\nSubcommand: " + code(selectedSubcommand))
				.append("\n\n")
				.append(bold(underline("Syntax")))
				.append("\n")
				.append(codeBlock(prefix + joinAliases(cmd) + (selectedSubcommand.isEmpty() ? "" : " " + selectedSubcommand) + " " + entry.getSyntax()))
				.append(entry.getDescription())
				.append("\n");
		if (!entry.getFlagInfo().isEmpty()) {
			sb.append("\n").append(bold(underline("Flags"))).append("\n");
			entry.getFlagInfo().forEach((name, info) -> {
				sb.append(code(flagPrefix + name + (info.getValueFormat().isBlank() ? "" : "=<" + info.getValueFormat() + ">")))
						.append(": ")
						.append(info.getDescription())
						.append("\n");
			});
		}
		if (cmd.getDocumentation().getEntries().size() > 1) {
			sb.append("\n").append(bold(underline("See Also"))).append("\n");
			cmd.getDocumentation().getEntries().forEach((otherPage, otherEntry) -> {
				if (otherPage.equals(selectedSubcommand)) {
					return;
				}
				sb.append(code(prefix + "help " + selectedCommand + (otherPage.isEmpty() ? "" : " " + otherPage)))
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
