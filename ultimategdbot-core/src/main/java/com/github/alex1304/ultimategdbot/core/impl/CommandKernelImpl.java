package com.github.alex1304.ultimategdbot.core.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.CommandKernel;
import com.github.alex1304.ultimategdbot.api.CommandPermissionDeniedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.rest.http.client.ClientException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class CommandKernelImpl implements CommandKernel {

	private final Bot bot;
	private final Map<String, Command> commands;
	private final Map<Command, Map<String, Command>> subCommands;
	private final Map<String, Set<Command>> commandsByPlugins;
	private final Map<String, ReplyMenu> openedReplyMenus;
	private final Map<ReplyMenu, Disposable> disposableMenus;
	
	public CommandKernelImpl(Bot bot, Map<String, Command> commands, Map<Command, Map<String, Command>> subCommands,
			Map<String, Set<Command>> commandsByPlugins) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = Collections.unmodifiableMap(Objects.requireNonNull(commands));
		this.subCommands = Collections.unmodifiableMap(Objects.requireNonNull(subCommands));
		this.commandsByPlugins = Collections.unmodifiableMap(Objects.requireNonNull(commandsByPlugins));
		this.openedReplyMenus = new ConcurrentHashMap<>();
		this.disposableMenus = new ConcurrentHashMap<>();
	}

	private final class ReplyMenu {
		final Context ctx;
		final Message msg;
		final Map<String, Function<Context, Mono<Void>>> menuItems;
		final boolean deleteOnReply;
		final boolean deleteOnTimeout;
		ReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems, boolean deleteOnReply, boolean deleteOnTimeout) {
			this.ctx = ctx;
			this.msg = msg;
			this.menuItems = menuItems;
			this.deleteOnReply = deleteOnReply;
			this.deleteOnTimeout = deleteOnTimeout;
		}
		String toKey() {
			return msg.getChannelId().asString() + ctx.getEvent().getMessage().getAuthor().get().getId().asString();
		}
		void complete() {
			msg.delete().onErrorResume(e -> Mono.empty()).subscribe();
			if (openedReplyMenus.get(toKey()) == this) {
				openedReplyMenus.remove(toKey());
				var d = disposableMenus.remove(this);
				if (d != null) {
					d.dispose();
				}
			}
		}
		void timeout() {
			if (deleteOnTimeout) {
				msg.delete().onErrorResume(e -> Mono.empty()).subscribe();
			}
			if (openedReplyMenus.get(toKey()) == this) {
				openedReplyMenus.remove(toKey());
				disposableMenus.remove(this);
			}
		}
	}

	@Override
	public void start() {
		// Command handler
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
				.filter(event -> event.getMessage().getContent().isPresent() && event.getMessage().getAuthor().isPresent())
				.flatMap(event -> ContextImpl.findPrefixUsed(bot, event)
						.map(prefixUsed -> Tuples.of(event, prefixUsed,
								parseCommandLine(event.getMessage().getContent().get().substring(prefixUsed.length())))))
				.filter(tuple -> tuple.getT3().isPresent())
				.map(tuple -> new ContextImpl(tuple.getT3().get().getT1(), tuple.getT1(), tuple.getT3().get().getT2(), bot, tuple.getT2()))
				.subscribe(ctx -> invokeCommand(ctx.getCommand(), ctx).subscribe());
		// Reply menu handler
		bot.getDiscordClients().flatMap(client -> client.getEventDispatcher().on(MessageCreateEvent.class))
				.filter(event -> event.getMessage().getContent().isPresent()
						&& event.getMessage().getAuthor().isPresent()
						&& openedReplyMenus.containsKey(event.getMessage().getChannelId().asString()
								+ event.getMessage().getAuthor().get().getId().asString()))
				.map(event -> Tuples.of(event, BotUtils.parseArgs(event.getMessage().getContent().get())))
				.subscribe(tuple -> {
					final var event = tuple.getT1();
					final var args = tuple.getT2();
					var replyMenu = openedReplyMenus.get(event.getMessage().getChannelId().asString()
							+ event.getMessage().getAuthor().get().getId().asString());
					var replyItem = args.get(0).toLowerCase();
					var action = replyMenu.menuItems.get(replyItem);
					if (action == null) {
						return;
					}
					var originalCommand = replyMenu.ctx.getCommand();
					var newCommand = Command.forkedFrom(originalCommand, action);
					var newCtx = new ContextImpl(newCommand, event, args, bot, "");
					invokeCommand(newCommand, newCtx).doOnSuccess(__ -> {
						replyMenu.complete();
						if (replyMenu.deleteOnReply) {
							event.getMessage().delete().doOnError(___ -> {}).subscribe();
						}
					}).subscribe();
				});
	}

	@Override
	public Optional<Tuple2<Command, List<String>>> parseCommandLine(String commandLine) {
		return parseCommandLine(BotUtils.parseArgs(commandLine));
	}

	@Override
	public Optional<Tuple2<Command, List<String>>> parseCommandLine(List<String> commandLine) {
		var cmd = commands.get(commandLine.get(0));
		if (cmd == null) {
			return Optional.empty();
		}
		return Optional.of(Tuples.of(cmd, commandLine)).map(tuple -> {
			var cmdTmp = tuple.getT1();
			var argsTmp = new ArrayList<>(commandLine);
			while (argsTmp.size() > 1) {
				var subcmd = subCommands.get(cmdTmp).get(argsTmp.get(1));
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
	public Map<String, Set<Command>> getCommandsGroupedByPlugins() {
		return commandsByPlugins;
	}

	@Override
	public Mono<Void> invokeCommand(Command cmd, Context ctx) {
		var actions = cmd.getErrorActions();
		return ctx.getEvent().getMessage().getChannel()
				.filter(c -> cmd.getChannelTypesAllowed().contains(c.getType()))
				.flatMap(c -> cmd.getPermissionLevel().isGranted(ctx))
				.flatMap(isGranted -> isGranted ? cmd.execute(ctx) : Mono.error(new CommandPermissionDeniedException()))
				.doOnError(CommandFailedException.class, error -> actions.getOrDefault(CommandFailedException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: " + e.getMessage()).subscribe();
						}).accept(error, ctx))
				.doOnError(CommandPermissionDeniedException.class, error -> actions.getOrDefault(CommandPermissionDeniedException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: You are not granted the privileges to run this command.").subscribe();
						}).accept(error, ctx))
				.doOnError(InvalidSyntaxException.class, error -> actions.getOrDefault(InvalidSyntaxException.class, (e, ctx0) -> {
							ctx0.reply(":no_entry_sign: Invalid syntax, this is not how the command works. Check out `" + ctx0.getPrefixUsed()
									+ "help " + ctx.getArgs().get(0) + "` if you need assistance.").subscribe();
						}).accept(error, ctx))
				.doOnError(ClientException.class, error -> actions.getOrDefault(ClientException.class, (e, ctx0) -> {
							var ce = (ClientException) e;
							var h = ce.getErrorResponse();
							var sj = new StringJoiner("", "```\n", "```\n");
							h.getFields().forEach((k, v) -> sj.add(k).add(": ").add(String.valueOf(v)).add("\n"));
							ctx0.reply(":no_entry_sign: Discord returned an error when executing this command: "
									+ "`" + ce.getStatus().code() + " " + ce.getStatus().reasonPhrase() + "`\n"
									+ sj.toString()
									+ "Make sure that I have sufficient permissions in this server and try again.")
							.subscribe();
						}).accept(error, ctx))
				.doOnError(error -> {
					if (error instanceof CommandFailedException || error instanceof CommandPermissionDeniedException
							|| error instanceof InvalidSyntaxException || error instanceof ClientException) {
						return;
					}
					actions.getOrDefault(error.getClass(), (e, ctx0) -> {
						ctx0.reply(":no_entry_sign: An internal error occured. A crash report has been sent to the developer. Sorry for the inconvenience.")
								.subscribe();
						ctx0.getBot().logStackTrace(ctx0, e).subscribe();
					}).accept(error, ctx);
				});
	}

	@Override
	public String openReplyMenu(Context ctx, Message msg, Map<String, Function<Context, Mono<Void>>> menuItems,
			boolean deleteOnReply, boolean deleteOnTimeout) {
		if (!msg.getAuthor().isPresent()) {
			return "";
		}
		var fixedMenuItems = new LinkedHashMap<String, Function<Context, Mono<Void>>>();
		menuItems.forEach((k, v) -> fixedMenuItems.put(k.toLowerCase(), v));
		var rm = new ReplyMenu(ctx, msg, fixedMenuItems, deleteOnReply, deleteOnTimeout);
		var key = rm.toKey();
		var existing = openedReplyMenus.get(key);
		if (existing != null) {
			existing.timeout();
		}
		openedReplyMenus.put(key, rm);
		disposableMenus.put(rm, Mono.delay(Duration.ofSeconds(bot.getReplyMenuTimeout())).subscribe(__ -> rm.timeout()));
		return key;
	}

	@Override
	public void closeReplyMenu(String identifier) {
		var rm = openedReplyMenus.get(identifier);
		if (rm == null) {
			return;
		}
		rm.msg.delete().onErrorResume(e -> Mono.empty()).subscribe();
		rm.ctx.getEvent().getMessage().delete().onErrorResume(e -> Mono.empty()).subscribe();
		rm.complete();
	}

}
