package com.github.alex1304.ultimategdbot.core;

import java.util.List;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.Command;
import com.github.alex1304.ultimategdbot.plugin.api.CommandExecutor;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;
import com.github.alex1304.ultimategdbot.plugin.api.PluginContainer;
import com.github.alex1304.ultimategdbot.plugin.api.UltimateGDBot;
import com.github.alex1304.ultimategdbot.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Plugin loader that loads implementations of bot commands
 * 
 * @author Alex1304
 *
 */
final class CommandPluginLoader extends PluginLoader<Command> {

	CommandPluginLoader() {
		super(PluginLoader.DEFAULT_PLUGIN_DIR + "commands/", Command.class);
	}

	@Override
	void bind(UltimateGDBot bot) {
		Objects.requireNonNull(bot).getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
				.subscribeOn(Schedulers.elastic())
				.filterWhen(event -> event.getMessage().getAuthor().map(u -> !u.isBot())) // Ignore bot accounts
				.subscribe(event -> {
					final var content = event.getMessage().getContent();
					if (!content.isPresent())
						return;

					final var text = content.get();
					bot.getDiscordClient().getSelf().subscribe(self -> {
						Flux.just(bot.getFullPrefix(), bot.getCanonicalPrefix(), self.getMention())
								.filter(prefix -> text.toLowerCase().startsWith(prefix.toLowerCase())).take(1)
								.subscribe(prefixUsed -> {
									List<String> args = Utils.extractArgs(text, prefixUsed);

									final var cmd = PluginContainer.ofCommands().get(args.get(0));

									if (cmd == null) {
										return; // Silently fails if the command does not exist
									}

									final var context = new DiscordContext(event, args.subList(1, args.size()));

									CommandHandler.handleDefault(CommandExecutor.execute(cmd, context), context);
								});
					});
				});
	}

}
