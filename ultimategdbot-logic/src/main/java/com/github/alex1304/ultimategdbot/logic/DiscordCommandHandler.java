package com.github.alex1304.ultimategdbot.logic;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

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
	private final ClassLoader classLoader;
	private final ConcurrentHashMap<String, DiscordCommand> commandMap;
	
	public DiscordCommandHandler(UltimateGDBot bot, ClassLoader classLoader) {
		this.bot = Objects.requireNonNull(bot);
		this.classLoader = Objects.requireNonNull(classLoader);
		this.commandMap = new ConcurrentHashMap<>();
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
							
							final var cmd = commandMap.get(argsArray[0]);
							
							if (cmd == null)
								return; // Silently fails if the command does not exist
							
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
		
		
		Flux.interval(Duration.ofSeconds(5)).subscribe(i -> {
			System.out.println("----- " + i + " -----");
			commandMap.clear();
			var commandLoader = ServiceLoader.load(DiscordCommand.class, classLoader);
			commandLoader.stream().forEach(cmd -> {
				commandMap.put(cmd.get().getName(), cmd.get());
				System.out.println("Loaded command: " + cmd.get().getName());
			});
			
		});
	}
}
