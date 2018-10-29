package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.Command;
import com.github.alex1304.ultimategdbot.plugin.api.CommandExecutor;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;
import com.github.alex1304.ultimategdbot.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.scheduler.Schedulers;

/**
 * Contains native commands, always available regardless of plugins.
 * 
 * @author Alex1304
 *
 */
class NativeCommandLoader {
	
	private Map<String, Command> nativeCommands;
	
	NativeCommandLoader() {
		this.nativeCommands = new HashMap<>();
		registerCommands();
	}
	
	private void put(String name, Command cmd) {
		nativeCommands.put(name, cmd);
	}
	
	void registerCommands() {
		put("plugin", ctx -> {
			var subcmd = new HashMap<String, Command>();
			subcmd.put("install", ctx0 -> Utils.messageOf("Installing..."));
			subcmd.put("test", ctx0 -> Utils.messageOf("Testing..."));
			subcmd.put("errorlol", ctx0 -> {
				throw new RuntimeException("Whoops lol");
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
				.filterWhen(event -> event.getMessage().getAuthor()
						.flatMap(a -> UltimateGDBot.getInstance().getOwner().map(o -> a.equals(o))))
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

					final var context = new DiscordContext(event, args.subList(1, args.size()));
					CommandHandler.handleDefault(CommandExecutor.execute(cmd, context), context);
				});
	}
}