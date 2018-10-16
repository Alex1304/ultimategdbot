package com.github.alex1304.ultimategdbot.logic;

import java.util.Arrays;
import java.util.Objects;
import java.util.ServiceLoader;

import com.github.alex1304.ultimategdbot.command.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
import com.github.alex1304.ultimategdbot.command.api.DiscordContext;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;

/**
 * Handles commands received by the bot. It uses the message create event from Discord to read commands.
 * Then, the proper command is executed according to the implementations given by a ServiceLoader of DiscordCommands.
 *
 * @author Alex1304
 *
 */
public final class DiscordCommandHandler {
	
	private final UltimateGDBot bot;
	private final ServiceLoader<DiscordCommand> commandLoader;
	
	public DiscordCommandHandler(UltimateGDBot bot, ServiceLoader<DiscordCommand> commandLoader) {
		this.bot = Objects.requireNonNull(bot);
		this.commandLoader = Objects.requireNonNull(commandLoader);
	}
	
	/**
	 * Binds the Discord message create event to the command handler so that commands can be executed.
	 */
	public void bind() {
		bot.getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
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
							
							final var cmd = commandLoader.stream()
									.map(ServiceLoader.Provider::get)
									.filter(c -> c.getName().equalsIgnoreCase(argsArray[0])).findAny();
							
							if (!cmd.isPresent())
								return; // Silently fails if the command does not exist
							
							final var context = new DiscordContext(event, Arrays.asList(argsArray).subList(1, argsArray.length));
							try {
								var spec = cmd.get().execute(context);
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
		/*
		Flux.interval(Duration.ofSeconds(5)).subscribe(i -> {
			System.out.println("----- " + i + " -----");
			commandLoader.reload();
			commandLoader.stream().forEach(cmd -> System.out.println("Loaded command: " + cmd.get().getName()));
		});*/
	}
}
