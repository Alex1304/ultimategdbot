package com.github.alex1304.ultimategdbot.core;

import java.util.Arrays;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.command.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
import com.github.alex1304.ultimategdbot.command.api.DiscordContext;
import com.github.alex1304.ultimategdbot.command.api.PluginContainer;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Plugin loader that loads implementations of bot commands
 * 
 * @author Alex1304
 *
 */
public final class CommandPluginLoader extends PluginLoader<DiscordCommand> {

	public CommandPluginLoader() {
		super("./plugins/", DiscordCommand.class);
	}
	
	@Override
	public void bind(UltimateGDBot bot) {
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
						.filter(prefix -> text.toLowerCase().startsWith(prefix.toLowerCase()))
						.take(1)
						.subscribe(prefixUsed -> {
							final var argsArray = text.split(" +"); // message contains at least the prefix, so it can't be an empty array
							argsArray[0] = argsArray[0].substring(prefixUsed.length()); // extract command name
							
							final var cmd = PluginContainer.ofCommands().get(argsArray[0]);
							
							if (cmd == null) {
								return; // Silently fails if the command does not exist
							}
							
							final var context = new DiscordContext(event, Arrays.asList(argsArray).subList(1, argsArray.length));
							try {
								var spec = cmd.execute(context);
								event.getMessage().getChannel().flatMap(c -> c.createMessage(spec)).subscribe();
							} catch (CommandFailedException e) {
								event.getMessage().getChannel().flatMap(c -> c.createMessage(":white_negative_cross_mark: " + e.getMessage())).subscribe();
							} catch (RuntimeException e) {
								event.getMessage().getChannel().flatMap(c -> c.createMessage(":white_negative_cross_mark: An internal error occured")).subscribe();
								e.printStackTrace();
							}
						});
			});
		});
	}

}
