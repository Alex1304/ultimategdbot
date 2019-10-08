package com.github.alex1304.ultimategdbot.core;

import java.util.Comparator;

import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Scope;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;

import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@CommandSpec(
	aliases = { "setup", "settings", "configure", "config" },
	shortDescription = "View and edit the bot configuration in this server.",
	permLevel = PermissionLevel.SERVER_ADMIN,
	scope = Scope.GUILD_ONLY
)
class SetupCommand {

	@CommandAction
	@CommandDoc("Lists all configuration entries available in the bot, listed in alphabetical order and grouped by plugins. "
			+ "Each entry has a unique name with a value associated to it. You can edit an entry using the `set` subcommand.")
	public Mono<Void> run(Context ctx) {
		var sb = new StringBuffer("Here you can configure the bot for this server. "
				+ "You can update a field by doing `" + ctx.getPrefixUsed() + "setup set <field> <value>`. "
				+ "Use `None` as value to reset a field.\n\n");
		var guildId = ctx.getEvent().getGuildId().map(Snowflake::asLong).orElse(0L);
		return ctx.getBot().getDatabase()
				.performTransactionWhen(session -> Flux.fromIterable(ctx.getBot().getPlugins())
						.sort(Comparator.comparing(Plugin::getName))
						.concatMap(plugin -> Flux.fromIterable(plugin.getGuildConfigurationEntries().entrySet())
								.flatMap(entry -> entry.getValue().getAsString(session, guildId)
										.map(str -> Tuples.of(entry.getKey(), str)))
								.collectSortedList(Comparator.comparing(Tuple2::getT1))
								.doOnNext(list -> {
									sb.append("**__").append(plugin.getName()).append("__**\n");
									if (list.isEmpty()) {
										sb.append("_(Nothing to configure here)_\n");
										return;
									}
									list.forEach(TupleUtils.consumer((key, value) -> {
										sb.append('`');
										sb.append(key);
										sb.append("`: ");
										sb.append(value);
										sb.append('\n');
									}));
									sb.append('\n');
								}))
						.then())
				.then(Mono.defer(() -> ctx.reply(sb.toString())))
				.then();
	}
	
	@CommandAction("set")
	@CommandDoc("Sets a new value to a configuration entry. The `key` corresponds to the name of the entry you want to edit, "
			+ "and `value` corresponds to the new value you want to assign to the said key. If you input an invalid value, "
			+ "the command will give you an error message with details. Otherwise, the new value will be saved and will respond "
			+ "with a success message.")
	public Mono<Void> runSet(Context ctx, String key, String value) {
		var guildId = ctx.getEvent().getGuildId().map(Snowflake::asLong).orElse(0L);
		return Flux.fromIterable(ctx.getBot().getPlugins())
				.map(Plugin::getGuildConfigurationEntries)
				.filter(map -> map.containsKey(key))
				.switchIfEmpty(Mono.error(new CommandFailedException("There is no configuration entry with key `" + key + "`.")))
				.next()
				.map(map -> map.get(key))
				.flatMapMany(entry -> ctx.getBot().getDatabase().performTransactionWhen(session -> entry.setFromString(session, value, guildId)))
				.onErrorMap(IllegalArgumentException.class, e -> new CommandFailedException("Cannot assign this value as `" + key + "`: " + e.getMessage()))
				.then(ctx.reply(":white_check_mark: Settings updated!"))
				.then();
	}
}
