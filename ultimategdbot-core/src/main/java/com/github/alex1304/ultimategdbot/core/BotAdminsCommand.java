package com.github.alex1304.ultimategdbot.core;

import static discord4j.core.retriever.EntityRetrievalStrategy.STORE_FALLBACK_REST;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDescriptor;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandPermission;
import com.github.alex1304.ultimategdbot.core.database.BotAdmins;

import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

@CommandDescriptor(
		aliases = "botadmins",
		shortDescription = "Manage users who have bot admin privileges."
)
@CommandPermission(level = PermissionLevel.BOT_OWNER)
class BotAdminsCommand {

	@CommandAction
	@CommandDoc("Lists all users that have admin privileges on the bot.")
	public Mono<Void> run(Context ctx) {
		return ctx.bot().database().query(BotAdmins.class, "from BotAdmins")
				.flatMap(admin -> ctx.bot().gateway()
						.withRetrievalStrategy(STORE_FALLBACK_REST)
						.getUserById(Snowflake.of(admin.getUserId())))
				.map(User::getTag)
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
	
	@CommandAction("grant")
	@CommandDoc("Grants bot admin access to a user.")
	public Mono<Void> runGrant(Context ctx, User user) {
		return ctx.bot().database().findByID(BotAdmins.class, user.getId().asLong())
				.flatMap(__ -> Mono.error(new CommandFailedException("This user is already an admin.")))
				.then(Mono.just(new BotAdmins())
						.doOnNext(newAdmin -> newAdmin.setUserId(user.getId().asLong()))
						.flatMap(ctx.bot().database()::save))
				.then(ctx.reply("**" + user.getTag() + "** is now a bot administrator!"))
				.then(ctx.bot().log("Bot administrator added: **" 
						+ user.getTag() + "** (" + user.getId().asString() + ")"))
				.then();
	}
	
	@CommandAction("revoke")
	@CommandDoc("Revokes bot admin access from a user.")
	public Mono<Void> runRevoke(Context ctx, User user) {
		return ctx.bot().database().findByID(BotAdmins.class, user.getId().asLong())
				.switchIfEmpty(Mono.error(new CommandFailedException("This user is already not an admin.")))
				.flatMap(ctx.bot().database()::delete)
				.then(ctx.reply("**" + user.getTag() + "** is no longer a bot administrator!"))
				.then(ctx.bot().log("Bot administrator removed: **" 
						+ user.getTag() + "** (" + user.getId().asString() + ")"))
				.then();
	}
}
