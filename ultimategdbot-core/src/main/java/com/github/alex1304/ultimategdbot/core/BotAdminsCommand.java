package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.CommandSpec;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.Subcommand;
import com.github.alex1304.ultimategdbot.api.command.argument.UserParser;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

@CommandSpec(aliases="botadmins", permLevel=PermissionLevel.BOT_OWNER)
class BotAdminsCommand {
	
	@Subcommand("grant")
	@CommandAction(UserParser.class)
	public Mono<Void> runGrant(Context ctx, User user) {
		return ctx.getBot().getDatabase().findByID(BotAdmins.class, user.getId().asLong())
				.flatMap(__ -> Mono.error(new CommandFailedException("This user is already an admin.")))
				.then(Mono.just(new BotAdmins())
						.doOnNext(newAdmin -> newAdmin.setUserId(user.getId().asLong()))
						.flatMap(ctx.getBot().getDatabase()::save))
				.then(ctx.reply("**" + BotUtils.formatDiscordUsername(user) + "** is now a bot administrator!"))
				.then(ctx.getBot().log("Bot administrator added: **" 
						+ BotUtils.formatDiscordUsername(user) + "** (" + user.getId().asString() + ")"))
				.then();
	}
	
	@Subcommand("revoke")
	@CommandAction(UserParser.class)
	public Mono<Void> runRevoke(Context ctx, User user) {
		return ctx.getBot().getDatabase().findByID(BotAdmins.class, user.getId().asLong())
				.switchIfEmpty(Mono.error(new CommandFailedException("This user is already not an admin.")))
				.flatMap(ctx.getBot().getDatabase()::delete)
				.then(ctx.reply("**" + BotUtils.formatDiscordUsername(user) + "** is no longer a bot administrator!"))
				.then(ctx.getBot().log("Bot administrator removed: **" 
						+ BotUtils.formatDiscordUsername(user) + "** (" + user.getId().asString() + ")"))
				.then();
	}
	
	@Subcommand("list")
	@CommandAction
	public Mono<Void> runList(Context ctx) {
		return ctx.getBot().getDatabase().query(BotAdmins.class, "from BotAdmins")
				.flatMap(admin -> ctx.getBot().getDiscordClients().next().flatMap(client -> client.getUserById(Snowflake.of(admin.getUserId()))))
				.map(BotUtils::formatDiscordUsername)
				.collectSortedList(String.CASE_INSENSITIVE_ORDER)
				.map(adminList -> {
					var sb = new StringBuilder("__**Bot administrator list:**__\n\n");
					adminList.forEach(admin -> sb.append(admin).append("\n"));
					if (adminList.isEmpty()) {
						sb.append("*(No data)*\n");
					}
					return sb.toString().substring(0, Math.min(sb.toString().length(), 800));
				})
				.flatMap(ctx::reply)
				.then();
	}
}
