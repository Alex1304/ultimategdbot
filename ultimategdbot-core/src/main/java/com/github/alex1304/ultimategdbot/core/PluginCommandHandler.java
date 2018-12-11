package com.github.alex1304.ultimategdbot.core;

import java.util.List;
import java.util.Objects;

import com.github.alex1304.ultimategdbot.plugin.api.Bot;
import com.github.alex1304.ultimategdbot.plugin.api.BotRoles;
import com.github.alex1304.ultimategdbot.plugin.api.CommandContainer;
import com.github.alex1304.ultimategdbot.plugin.api.DiscordContext;
import com.github.alex1304.ultimategdbot.utils.Utils;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Listens to Discord create message events in order to execute the commands
 * provided by the plugins.
 * 
 * @author Alex1304
 */
class PluginCommandHandler {

	/**
	 * Defines the way that the bot should execute code loaded from plugins.
	 */
	void bind(Bot bot) {
		Objects.requireNonNull(bot).getDiscordClient().getEventDispatcher().on(MessageCreateEvent.class)
				.subscribeOn(Schedulers.elastic())
				.filterWhen(event -> event.getMessage().getAuthor().map(u -> !u.isBot())) // Ignore bot accounts
				.filterWhen(event -> event.getMessage().getChannel().map(c -> c.getType() == Type.GUILD_TEXT))
				.subscribe(event -> {
					final var content = event.getMessage().getContent();
					if (!content.isPresent())
						return;

					final var text = content.get();
					bot.getDiscordClient().getSelf().subscribe(self -> {
						Flux.just(bot.getPrefix(), self.getMention())
								.filter(prefix -> text.toLowerCase().startsWith(prefix.toLowerCase())).take(1)
								.subscribe(prefixUsed -> {
									List<String> args = Utils.extractArgs(text, prefixUsed);
									final var cmd = CommandContainer.getInstance().get(args.get(0));

									if (cmd == null) {
										return; // Silently fails if the command does not exist
									}

									event.getGuild().subscribe(g -> {
										BotRoles.isGranted(bot, event.getMessage().getAuthor(),
												g.getChannelById(event.getMessage().getChannelId()),
												cmd.getRoleRequired()).subscribe(isGranted -> {
													if (!isGranted) {
														event.getMessage().getChannel().flatMap(c -> c.createMessage(
																":no_entry_sign: You don't have permission to use this command."))
																.subscribe();
														return;
													}

													final var ctx = new DiscordContext(bot, event,
															args.subList(1, args.size()), prefixUsed, args.get(0));

													try {
														cmd.execute(ctx).doOnError(e -> {
															event.getMessage().getChannel()
																	.flatMap(c -> c.createMessage(
																			":no_entry_sign: " + e.getMessage()))
																	.subscribe();
														}).subscribe();
													} catch (RuntimeException e) {
														event.getMessage().getChannel()
																.flatMap(c -> c.createMessage(
																		":no_entry_sign: An internal error occured"))
																.subscribe();
														e.printStackTrace();
													}
												});
									});
								});
					});
				});
	}
}
