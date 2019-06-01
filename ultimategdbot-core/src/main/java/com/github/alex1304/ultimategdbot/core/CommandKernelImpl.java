package com.github.alex1304.ultimategdbot.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

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
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.retry.Retry;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

class CommandKernelImpl implements CommandKernel {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("ultimategdbot.commandkernel");

	private final Bot bot;
	private final Map<String, Command> commands;
	private final Map<Command, Map<String, Command>> subCommands;
	private final CommandErrorHandler globalErrorHandler;
	private final Set<Long> blacklist;
	
	public CommandKernelImpl(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = new ConcurrentHashMap<>();
		this.subCommands = new ConcurrentHashMap<>();
		this.globalErrorHandler = new CommandErrorHandler();
		this.blacklist = new HashSet<>();
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
			LOGGER.debug("Discord ClientException thrown when using a command. User input: "
					+ ctx.getEvent().getMessage().getContent().orElse("") + ", Error:", e);
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
				BotUtils.debugError(":no_entry_sign: **A database error occured.**", ctx, e)).then());
		// Parse and execute commands from message create events
		bot.getDiscordClients()
				.doOnNext(client -> {
					var shardLogger = LoggerFactory.getLogger("ultimategdbot.commandkernel." + client.getConfig().getShardIndex());
					client.getEventDispatcher().on(MessageCreateEvent.class)
							.filter(event -> event.getMessage().getAuthor().map(User::isBot).map(b -> !b).orElse(false))
							.flatMap(event -> {
								var content = event.getMessage().getContent().orElse("");
								return findPrefixUsed(bot, event)
										.filter(prefix -> content.length() >= prefix.length())
										.map(prefix -> parseCommandLine(content.substring(prefix.length()).strip())
												.map(TupleUtils.function((cmd, args) -> new Context(cmd, event, args, bot, prefix))))
										.flatMap(Mono::justOrEmpty);
							})
							.doOnNext(ctx -> shardLogger.debug("Command invoked by user: {}", ctx))
							.flatMap(ctx -> invokeCommand(ctx.getCommand(), ctx)
									.materialize()
									.elapsed()
									.doOnNext(TupleUtils.consumer((time, signal) -> {
										var duration = BotUtils.formatTimeMillis(Duration.ofMillis(time));
										if (signal.isOnComplete()) {
											shardLogger.debug("Command completed with success in {}: {}", duration, ctx);
										} else if (signal.getThrowable() instanceof HandledCommandException) {
											shardLogger.debug("Command completed with {} in {}: {}", signal.getThrowable().getCause()
													.getClass().getCanonicalName(), duration, ctx);
										}
									}))
									.map(Tuple2::getT2)
									.dematerialize()
									.onErrorResume(HandledCommandException.class, e -> Mono.empty())
									.onErrorResume(e -> Mono.fromRunnable(() -> shardLogger
											.error("A command threw an unhandled exception: " + ctx, e))))
							.retryWhen(Retry.any().doOnRetry(retryCtx -> shardLogger
									.error("A critical error occured in the command handler. Recovering.", retryCtx.exception())))
							.subscribe();
				}).subscribe();
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
		return getBlacklistState(ctx)
				.map(blacklist -> Mono.<Void>fromRunnable(() -> LOGGER
						.debug("Blacklisted {} attempted to use command {}", blacklist, cmd.getClass().getCanonicalName())))
				.defaultIfEmpty(Mono.defer(() -> {
					var pluginSpecificErrorHandler = cmd.getPlugin().getCommandErrorHandler();
					return globalErrorHandler.apply(pluginSpecificErrorHandler.apply(ctx.getEvent().getMessage().getChannel()
							.filter(c -> cmd.getChannelTypesAllowed().contains(c.getType()))
							.flatMap(c -> cmd.getPermissionLevel().isGranted(ctx))
							.flatMap(isGranted -> isGranted ? cmd.execute(ctx) 
									: Mono.error(new CommandPermissionDeniedException())), ctx), ctx)
							.onErrorResume(error -> !(error instanceof HandledCommandException), error -> ctx
									.reply(":no_entry_sign: Something went wrong. A crash report has been sent "
											+ "to the developer. Sorry for the inconvenience.")
									.onErrorResume(e -> Mono.empty())
									.thenMany(BotUtils.debugError(":no_entry_sign: **Something went wrong while "
											+ "executing a command.**", ctx, error))
									.then(Mono.error(error)));
				}))
				.flatMap(Function.identity());
	}

	@Override
	public Set<Long> getBlacklist() {
		return Collections.unmodifiableSet(blacklist);
	}

	@Override
	public void blacklist(long id) {
		blacklist.add(id);
	}

	@Override
	public void unblacklist(long id) {
		blacklist.remove(id);
	}
	
	private static Mono<String> findPrefixUsed(Bot bot, MessageCreateEvent event) {
		var msgContent = event.getMessage().getContent().orElse("");
		return Mono.justOrEmpty(event.getGuildId())
				.map(Snowflake::asLong)
				.flatMap(guildId -> bot.getDatabase().findByID(NativeGuildSettings.class, guildId)
						.switchIfEmpty(Mono.fromCallable(() -> {
									var gs = new NativeGuildSettings();
									gs.setGuildId(guildId);
									return gs;
								})
								.flatMap(gs -> bot.getDatabase().save(gs)
										.then(Mono.fromRunnable(() -> LOGGER.debug("Created guild settings: {}", gs)))
										.onErrorResume(e -> Mono.fromRunnable(() -> LOGGER
												.error("Unable to save guild settings for " + guildId, e)))
										.thenReturn(gs)))
						.flatMap(gs -> Mono.justOrEmpty(gs.getPrefix())))
				.defaultIfEmpty(bot.getDefaultPrefix())
				.map(String::strip)
				.map(specificPrefix -> bot.getMainDiscordClient().getSelfId()
						.map(Snowflake::asString)
						.stream()
						.flatMap(botId -> Stream.of("<@" + botId + ">", "<@!" + botId + ">", specificPrefix)
								.filter(prefix -> prefix.equalsIgnoreCase(msgContent
										.substring(0, Math.min(prefix.length(), msgContent.length())))))
						.findFirst())
				.flatMap(Mono::justOrEmpty);
	}
	
	private Mono<BlacklistType> getBlacklistState(Context ctx) {
		return ctx.getBot().getApplicationInfo()
				.flatMap(appInfo -> Mono.justOrEmpty(ctx.getEvent().getMessage().getAuthor()
						.map(User::getId)
						.map(Snowflake::asLong)
						.filter(a -> appInfo.getOwnerId().asLong() != a)
						.flatMap(a -> ctx.getEvent().getGuildId()
								.map(Snowflake::asLong)
								.map(g -> {
									if (blacklist.contains(ctx.getEvent().getMessage().getChannelId().asLong())) {
										return BlacklistType.CHANNEL;
									} else if (blacklist.contains(g)) {
										return BlacklistType.GUILD;
									} else if (blacklist.contains(a)) {
										return BlacklistType.USER;
									} else {
										return null;
									}
								}))));
	}
	
	void addCommands(Map<String, Command> commands) {
		this.commands.putAll(commands);
	}
	
	void addSubcommands(Map<Command, Map<String, Command>> subCommands) {
		this.subCommands.putAll(subCommands);
	}
	
	private enum BlacklistType {
		USER, CHANNEL, GUILD;
	}
}
