package com.github.alex1304.ultimategdbot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.Command;
import com.github.alex1304.ultimategdbot.plugin.api.CommandExecutor;
import com.github.alex1304.ultimategdbot.plugin.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;
import com.github.alex1304.ultimategdbot.plugin.api.PluginContainer;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;
import com.github.alex1304.ultimategdbot.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.scheduler.Schedulers;

/**
 * Contains native commands, always available regardless of plugins. Native
 * commands can manage plugins, that's why plugin loaders need to be supplied
 * when instanciating a NativeCommandLoader.
 * 
 * @author Alex1304
 *
 */
class NativeCommandLoader {

	private final Map<String, Command> nativeCommands;
	private final CommandPluginLoader cmdLoader;
	private final ServicePluginLoader srvLoader;

	NativeCommandLoader(CommandPluginLoader cmdLoader, ServicePluginLoader srvLoader) {
		this.nativeCommands = new HashMap<>();
		this.cmdLoader = cmdLoader;
		this.srvLoader = srvLoader;
		registerCommands();
	}

	private void put(String name, Command cmd) {
		nativeCommands.put(name, cmd);
	}

	void registerCommands() {
		put("ping", ctx -> Utils.messageOf(":ping_pong: Pong!"));

		put("plugin", ctx -> {
			var subcmd = new HashMap<String, Command>();
			subcmd.put("load", ctx0 -> {
				final var pluginsRemoved = new ArrayList<String>();
				final var pluginsAdded = new ArrayList<String>();
				PluginContainer.ofCommands().forEach(plugin -> pluginsRemoved.add("Command: " + plugin.getName()));
				PluginContainer.ofServices().forEach(plugin -> pluginsRemoved.add("Service: " + plugin.getName()));
				cmdLoader.loadInto(PluginContainer.ofCommands());
				srvLoader.loadInto(PluginContainer.ofServices());
				PluginContainer.ofCommands().forEach(plugin -> pluginsAdded.add("Command: " + plugin.getName()));
				PluginContainer.ofServices().forEach(plugin -> pluginsAdded.add("Service: " + plugin.getName()));

				final var pluginsRemovedCpy = new ArrayList<>(pluginsRemoved);
				pluginsRemoved.removeIf(p -> pluginsAdded.contains(p));
				pluginsAdded.removeIf(p -> pluginsRemovedCpy.contains(p));

				final var sb = new StringBuilder();
				if (pluginsAdded.isEmpty() && pluginsRemoved.isEmpty()) {
					sb.append("All plugins already loaded! No action needed.");
				} else {
					if (!pluginsAdded.isEmpty()) {
						sb.append("**New plugins loaded:**\n```\n");
						pluginsAdded.forEach(p -> {
							sb.append(p);
							sb.append('\n');
						});
						sb.append("```\n");
					}

					if (!pluginsRemoved.isEmpty()) {
						sb.append("**Removed plugins:**\n```\n");
						pluginsRemoved.forEach(p -> {
							sb.append(p);
							sb.append('\n');
						});
						sb.append("```\n");
					}
				}

				return Utils.messageOf(sb.toString());
			});
			return CommandExecutor.executeSubcommand(subcmd, ctx);
		});

		put("service", ctx -> {
			return Utils.messageOf("No services yet. Soon:tm:");
		});

		put("errorlol", ctx -> {
			throw new RuntimeException("Whoops lol");
		});
	}

	void bind(UltimateGDBot bot) {
		Objects.requireNonNull(bot).getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
				.subscribeOn(Schedulers.elastic())
				.filterWhen(event -> event.getMessage().getAuthor().flatMap(a -> bot.getOwner().map(o -> a.equals(o))))
				.filterWhen(event -> event.getMessage().getChannel().map(c -> c.getType() == Type.DM))
				.subscribe(event -> {
					final var content = event.getMessage().getContent();
					if (!content.isPresent())
						return;

					final var text = content.get();
					List<String> args = Utils.extractArgs(text, "");

					final var cmd = nativeCommands.get(args.get(0));

					if (cmd == null) {
						return; // Silently fails if the command does not exist
					}

					final var ctx = new DiscordContext(bot, event, args.subList(1, args.size()), "", args.get(0));
					CommandExecutor.execute(cmd, ctx).doOnError(e -> {
						if (e instanceof CommandFailedException) {
							ctx.getEvent().getMessage().getChannel()
									.flatMap(c -> c.createMessage(":no_entry_sign: " + e.getMessage())).subscribe();
						} else {
							ctx.getEvent().getMessage().getChannel()
									.flatMap(c -> c.createMessage(":no_entry_sign: An internal error occured"))
									.subscribe();
							e.printStackTrace();
						}
					}).subscribe(mcs -> ctx.getEvent().getMessage().getChannel().flatMap(c -> c.createMessage(mcs))
							.subscribe());
				});
	}
}