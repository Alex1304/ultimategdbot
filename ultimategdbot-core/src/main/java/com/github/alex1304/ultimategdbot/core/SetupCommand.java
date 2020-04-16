package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Scope;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandPermission;

@CommandDescriptor(
	aliases = { "setup", "settings", "configure", "config" },
	shortDescription = "View and edit the bot configuration in this server.",
	scope = Scope.GUILD_ONLY
)
@CommandPermission(level = PermissionLevel.GUILD_ADMIN)
class SetupCommand {

//	@CommandAction
//	@CommandDoc("Lists all configuration entries available in the bot, listed in alphabetical order and grouped by plugins. "
//			+ "Each entry has a unique name with a value associated to it. You can edit an entry using the `set` subcommand.")
//	public Mono<Void> run(Context ctx) {
//		var sb = new StringBuffer("Here you can configure the bot for this server. "
//				+ "You can update a field by doing `" + ctx.prefixUsed() + "setup set <field> <value>`. "
//				+ "Use `None` as value to reset a field.\n\n");
//		var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElse(0L);
//		return ctx.bot().database()
//				.performTransactionWhen(session -> Flux.fromIterable(ctx.bot().plugins())
//						.sort(Comparator.comparing(Plugin::getName))
//						.concatMap(plugin -> Flux.fromIterable(plugin.getGuildConfigurationEntries().entrySet())
//								.flatMap(entry -> entry.getValue().getAsString(session, guildId)
//										.map(str -> Tuples.of(entry.getKey(), str)))
//								.collectSortedList(Comparator.comparing(Tuple2::getT1))
//								.doOnNext(list -> {
//									sb.append("**__").append(plugin.getName()).append("__**\n");
//									if (list.isEmpty()) {
//										sb.append("_(Nothing to configure here)_\n");
//										return;
//									}
//									list.forEach(TupleUtils.consumer((key, value) -> {
//										sb.append('`');
//										sb.append(key);
//										sb.append("`: ");
//										sb.append(value);
//										sb.append('\n');
//									}));
//									sb.append('\n');
//								}))
//						.then())
//				.then(Mono.defer(() -> ctx.reply(sb.toString())))
//				.then();
//	}
//	
//	@CommandAction("set")
//	@CommandDoc("Sets a new value to a configuration entry. The `key` corresponds to the name of the entry you want to edit, "
//			+ "and `value` corresponds to the new value you want to assign to the said key. If you input an invalid value, "
//			+ "the command will give you an error message with details. Otherwise, the new value will be saved and will respond "
//			+ "with a success message.")
//	public Mono<Void> runSet(Context ctx, String key, String value) {
//		var guildId = ctx.event().getGuildId().map(Snowflake::asLong).orElse(0L);
//		return Flux.fromIterable(ctx.bot().plugins())
//				.map(Plugin::getGuildConfigurationEntries)
//				.filter(map -> map.containsKey(key))
//				.switchIfEmpty(Mono.error(new CommandFailedException("There is no configuration entry with key `" + key + "`.")))
//				.next()
//				.map(map -> map.get(key))
//				.flatMapMany(entry -> ctx.bot().database().performTransactionWhen(session -> entry.setFromString(session, value, guildId)))
//				.onErrorMap(IllegalArgumentException.class, e -> new CommandFailedException("Cannot assign this value as `" + key + "`: " + e.getMessage()))
//				.then(ctx.reply(":white_check_mark: Settings updated!"))
//				.then();
//	}
}
