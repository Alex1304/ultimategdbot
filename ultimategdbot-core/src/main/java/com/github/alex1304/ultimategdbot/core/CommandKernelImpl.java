package com.github.alex1304.ultimategdbot.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandErrorHandler;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.CommandKernel;
import com.github.alex1304.ultimategdbot.api.CommandPermissionDeniedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.DatabaseException;
import com.github.alex1304.ultimategdbot.api.HandledCommandException;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

class CommandKernelImpl implements CommandKernel {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandKernelImpl.class);

	private final Bot bot;
	private final Map<String, Command> commands;
	private final Map<Command, Map<String, Command>> subCommands;
	private final CommandErrorHandler globalErrorHandler;
	
	public CommandKernelImpl(Bot bot, Map<String, Command> commands, Map<Command, Map<String, Command>> subCommands) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = Collections.unmodifiableMap(Objects.requireNonNull(commands));
		this.subCommands = Collections.unmodifiableMap(Objects.requireNonNull(subCommands));
		this.globalErrorHandler = new CommandErrorHandler();
	}

	public void start() {
		// Global error handler
		globalErrorHandler.addHandler(CommandFailedException.class, (e, ctx) -> ctx.reply(":no_entry_sign: " + e.getMessage()).then());
		globalErrorHandler.addHandler(InvalidSyntaxException.class, (e, ctx) -> ctx.reply(":no_entry_sign: Invalid syntax!"
				+ "\n```\n" + ctx.getPrefixUsed() + ctx.getArgs().get(0) + " " + ctx.getCommand().getSyntax()
				+ "\n```\n" + "See `" + ctx.getPrefixUsed() + "help " + ctx.getArgs().get(0) + "` for more information.").then());
		globalErrorHandler.addHandler(CommandPermissionDeniedException.class, (e, ctx) ->
				ctx.reply(":no_entry_sign: You are not granted the privileges to run this command.").then());
		globalErrorHandler.addHandler(ClientException.class, (e, ctx) -> {
			LOGGER.debug("Discord ClientException thrown when using a command. User input: {}, Error: {}",
					ctx.getEvent().getMessage().getContent().orElse(""), e);
			var h = e.getErrorResponse();
			var sj = new StringJoiner("", "```\n", "```\n");
			h.getFields().forEach((k, v) -> sj.add(k).add(": ").add(String.valueOf(v)).add("\n"));
			return ctx.reply(":no_entry_sign: Discord returned an error when executing this command: "
							+ "`" + e.getStatus().code() + " " + e.getStatus().reasonPhrase() + "`\n"
							+ sj.toString()
							+ (e.getStatus().code() == 403 ? "Make sure that I have sufficient permissions in this server and try again." : ""))
					.then();
		});
		globalErrorHandler.addHandler(DatabaseException.class, (e, ctx) -> Flux.merge(
				ctx.reply(":no_entry_sign: An error occured when accessing the database. Try again."),
				bot.log(":no_entry_sign: A database error occured.\nContext dump: `" + ctx + "`\nException:`"
						+ e.getCause().getClass().getCanonicalName() + ": "
						+ Optional.ofNullable(e.getCause().getMessage()).orElse("*no message*"))).then());
		// Parse and execute commands from message create events
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
				.filter(event -> event.getMessage().getContent().isPresent()
						&& event.getMessage().getAuthor().isPresent()
						&& !event.getMessage().getAuthor().get().isBot())
				.flatMap(event -> findPrefixUsed(bot, event).map(prefixUsed -> Tuples.of(event, prefixUsed,
						parseCommandLine(event.getMessage().getContent().get().substring(prefixUsed.length())))))
				.filter(tuple -> tuple.getT3().isPresent())
				.map(tuple -> new Context(tuple.getT3().get().getT1(), tuple.getT1(), tuple.getT3().get().getT2(), bot, tuple.getT2()))
				.flatMap(ctx -> invokeCommand(ctx.getCommand(), ctx))
				.onErrorContinue(HandledCommandException.class, (error, obj) -> LOGGER.debug("Successfully handled command exception", error))
				.onErrorContinue((error, obj) -> LOGGER.error("An error occured when processing a MessageCreateEvent on " + obj, error))
				.subscribe();
	}

	@Override
	public Optional<Tuple2<Command, List<String>>> parseCommandLine(String commandLine) {
		return parseCommandLine(BotUtils.parseArgs(commandLine));
	}

	@Override
	public Optional<Tuple2<Command, List<String>>> parseCommandLine(List<String> commandLine) {
		if (commandLine.isEmpty()) {
			return Optional.empty();
		}
		var cmd = commands.get(commandLine.get(0).toLowerCase());
		if (cmd == null) {
			return Optional.empty();
		}
		return Optional.of(Tuples.of(cmd, commandLine)).map(tuple -> {
			var cmdTmp = tuple.getT1();
			var argsTmp = new ArrayList<>(commandLine);
			while (argsTmp.size() > 1) {
				var subcmd = subCommands.get(cmdTmp).get(argsTmp.get(1).toLowerCase());
				if (subcmd == null) {
					break;
				}
				cmdTmp = subcmd;
				var arg1 = argsTmp.remove(0);
				var arg2 = argsTmp.remove(0);
				argsTmp.add(0, String.join(" ", arg1, arg2));
			}
			return Tuples.of(cmdTmp, argsTmp);
		});
	}

	@Override
	public Set<Command> getCommands() {
		return Collections.unmodifiableSet(new HashSet<>(commands.values()));
	}

	@Override
	public Mono<Void> invokeCommand(Command cmd, Context ctx) {
		var pluginSpecificErrorHandler = cmd.getPlugin().getCommandErrorHandler();
		return globalErrorHandler.apply(pluginSpecificErrorHandler.apply(ctx.getEvent().getMessage().getChannel()
				.filter(c -> cmd.getChannelTypesAllowed().contains(c.getType()))
				.flatMap(c -> cmd.getPermissionLevel().isGranted(ctx))
				.flatMap(isGranted -> isGranted ? cmd.execute(ctx) : Mono.error(new CommandPermissionDeniedException())), ctx), ctx)
				.onErrorResume(error -> !(error instanceof HandledCommandException), error -> ctx
						.reply(":no_entry_sign: Something went wrong. A crash report has been sent to the developer. Sorry for the inconvenience.")
						.onErrorResume(e -> Mono.empty())
						.then(ctx.getBot().logStackTrace(ctx, error))
						.then(Mono.error(error)));
	}
	
	private static Mono<String> findPrefixUsed(Bot bot, MessageCreateEvent event) {
		var guildIdOpt = event.getGuildId();
		var guildSettings = guildIdOpt.isPresent() ? bot.getDatabase().findByIDOrCreate(NativeGuildSettings.class, guildIdOpt.get().asLong(), (gs, gid) -> {
			gs.setGuildId(gid);
			gs.setPrefix(bot.getDefaultPrefix());
		}) : Mono.<NativeGuildSettings>empty();
		return Mono.just(bot.getMainDiscordClient().getSelfId())
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(botId -> Tuples.of("<@" + botId.asString() + "> ", "<@!" + botId.asString() + "> ",
						BotUtils.removeQuotesUnlessEscaped(event.getMessage().getContent().orElse(""))))
				.filter(tuple -> !tuple.getT3().isEmpty())
				.flatMap(tuple -> guildSettings
						.map(gs -> Tuples.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), gs.getPrefix() == null ? bot.getDefaultPrefix() : gs.getPrefix()))
						.defaultIfEmpty(Tuples.of(tuple.getT1(), tuple.getT2(), tuple.getT3(), bot.getDefaultPrefix())))
				.flatMap(tuple -> Flux.just(tuple.getT1(), tuple.getT2(), tuple.getT4())
							.filter(str -> str.equalsIgnoreCase(tuple.getT3().substring(0, Math.min(str.length(), tuple.getT3().length()))))
							.next());
	}
}
