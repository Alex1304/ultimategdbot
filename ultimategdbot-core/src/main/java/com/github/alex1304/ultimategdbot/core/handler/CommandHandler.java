package com.github.alex1304.ultimategdbot.core.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringJoiner;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.CommandPermissionDeniedException;
import com.github.alex1304.ultimategdbot.core.impl.ContextImpl;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Registers and executes commands when called.
 */
public class CommandHandler {

	private final Bot bot;
	private final Map<String, Command> commands;
	private final Set<Command> availableCommands;
	
	public CommandHandler(Bot bot) {
		this.bot = Objects.requireNonNull(bot);
		this.commands = new HashMap<>();
		this.availableCommands = new HashSet<>();
	}
	
	/**
	 * Loads commands from plugins.
	 */
	public void loadCommands() {
		var loader = ServiceLoader.load(Command.class);
		for (var cmd : loader) {
			System.out.printf("Loaded command: %s\n", cmd.getClass().getName());
			availableCommands.add(cmd);
			for (var alias : cmd.getAliases()) {
				commands.put(alias, cmd);
			}
		}
	}
	
	/**
	 * Listens to MessageCreateEvents to trigger commands.
	 */
	public void listen() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class).subscribeOn(Schedulers.elastic())
				.filter(event -> event.getMessage().getContent().isPresent())
				.map(event -> new ContextImpl(event, Arrays.asList(event.getMessage().getContent().get().split(" +")), bot))
				.filter(ctx -> ctx.getArgs().get(0).startsWith(ctx.getGuildSettings() != null ? ctx.getGuildSettings().getPrefix() : bot.getDefaultPrefix()))
				.subscribe(ctx -> {
					var prefix = ctx.getGuildSettings() != null ? ctx.getGuildSettings().getPrefix() : bot.getDefaultPrefix();
					var cmdName = ctx.getArgs().get(0).substring(prefix.length());
					var cmd = commands.get(cmdName);
					if (cmd == null) {
						return;
					}
					cmd.getPermissionLevel().isGranted(ctx)
							.filterWhen(__ -> ctx.getEvent().getMessage().getChannel().map(c -> cmd.getChannelTypesAllowed().contains(c.getType())))
							.flatMap(isGranted -> {
								if (!isGranted) {
									return Mono.error(new CommandPermissionDeniedException());
								} else {
									return cmd.execute(ctx);
								}
							}).doOnError(error -> {
								var actions = cmd.getErrorActions();
								if (error instanceof CommandFailedException) {
									actions.getOrDefault(CommandFailedException.class, (e, ctx0) -> {
										ctx0.reply(":no_entry_sign: " + e.getMessage()).subscribe();
									}).accept(error, ctx);
								} else if (error instanceof CommandPermissionDeniedException) {
									actions.getOrDefault(CommandFailedException.class, (e, ctx0) -> {
										ctx0.reply(":no_entry_sign: You don't have the required permissions to run this command.").subscribe();
									}).accept(error, ctx);
								} else if (error instanceof ClientException) {
									actions.getOrDefault(ClientException.class, (e, ctx0) -> {
										var ce = (ClientException) e;
										var h = ce.getErrorResponse();
										var sj = new StringJoiner("", "```\n", "```\n");
										h.getFields().forEach((k, v) -> sj.add(k).add(": ").add(String.valueOf(v)).add("\n"));
										ctx0.reply(":no_entry_sign: Discord returned an error when executing this command: "
												+ "`" + ce.getStatus().code() + " " + ce.getStatus().reasonPhrase() + "`\n"
												+ sj.toString()
												+ "Make sure that I have sufficient permissions in this server and try again.")
										.subscribe();
									}).accept(error, ctx);
								} else {
									actions.getOrDefault(error.getClass(), (e, ctx0) -> {
										ctx0.reply(":no_entry_sign: An internal error occured. A crash report has been sent to the developer.").subscribe();
									}).accept(error, ctx);
								}
							}).subscribe();
				});
	}

	/**
	 * Gets a set of available commands.
	 * 
	 * @return the available commands
	 */
	public Set<Command> getAvailableCommands() {
		return availableCommands;
	}
}
