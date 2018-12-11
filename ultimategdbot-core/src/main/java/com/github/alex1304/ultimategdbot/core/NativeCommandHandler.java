package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.Bot;
import com.github.alex1304.ultimategdbot.plugin.api.Command;
import com.github.alex1304.ultimategdbot.plugin.api.CommandContainer;
import com.github.alex1304.ultimategdbot.plugin.api.CommandExecutor;
import com.github.alex1304.ultimategdbot.plugin.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;
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
class NativeCommandHandler {

	private final Map<String, Command> nativeCommands;

	NativeCommandHandler(PluginCommandHandler cmdLoader) {
		this.nativeCommands = new HashMap<>();
		registerCommands();
	}

	private void put(String name, Command cmd) {
		nativeCommands.put(name, cmd);
	}

	void registerCommands() {
		put("plugin", ctx -> {
			var subcmd = new HashMap<String, Command>();

			subcmd.put("list", ctx0 -> {
				final var container = CommandContainer.getInstance();
				final var sb = new StringBuilder("**Plugin list:\n```\n");
				container.forEach(cmd -> sb.append(cmd.getName()).append(" [").append(cmd.getClass().getName())
						.append(']').append('\n'));
				return Utils.reply(ctx0.getEvent(), sb.append("```").toString()).then();
			});

			return CommandExecutor.executeSubcommand(subcmd, ctx);
		});
	}

	void bind(Bot bot) {
		Objects.requireNonNull(bot).getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
				.subscribeOn(Schedulers.elastic())
				.filterWhen(event -> event.getMessage().getAuthor()
						.flatMap(a -> bot.getDiscordClient().getApplicationInfo()
								.flatMap(ai -> ai.getOwner().map(o -> a.equals(o)))))
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
					try {
						cmd.execute(ctx).doOnError(e -> {
							event.getMessage().getChannel()
									.flatMap(c -> c.createMessage(
											":no_entry_sign: " + e.getMessage()))
									.subscribe();
							if (!(e instanceof CommandFailedException)) {
								e.printStackTrace();
							}
						}).subscribe();
					} catch (RuntimeException e) {
						event.getMessage().getChannel()
								.flatMap(c -> c.createMessage(
										":no_entry_sign: An internal error occured"))
								.subscribe();
						e.printStackTrace();
					}
				});
	}
}