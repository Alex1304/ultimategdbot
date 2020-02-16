package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandDoc;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.core.database.BlacklistedIds;

import reactor.core.publisher.Mono;

@CommandSpec(
		aliases = "blacklist",
		shortDescription = "Restrict guilds, channels or users from using the bot.",
		requiredPermissionLevel = PermissionLevel.BOT_ADMIN
)
class BlacklistCommand {

	@CommandAction("add")
	@CommandDoc("Adds an ID to the blacklist. The ID may refer to a guild, a guild channel or a user in Discord. "
			+ "If someone attempts to run a command while their ID or the ID of the guild/channel they're using "
			+ "the command in is blacklisted, the command will be ignored without any side effect. This command "
			+ "is useful to handle cases of abuse.")
	public Mono<Void> runAdd(Context ctx, long id) {
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.flatMap(__ -> Mono.error(new CommandFailedException("This ID is already blacklisted")))
				.then(Mono.fromCallable(() -> {
							var b = new BlacklistedIds();
							b.setId(id);
							return b;
						}).flatMap(ctx.getBot().getDatabase()::save))
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().blacklist(id)))
				.then(ctx.reply("**" + id + "** is now blacklisted!")
						.and(ctx.getBot().log("ID added to blacklist: " + id)));
	}

	@CommandAction("remove")
	@CommandDoc("Removes an ID from the blacklist. Once an ID is removed from the blacklist, the user/channel/guild "
			+ "in question will be able to run bot commands again normally.")
	public Mono<Void> runRemove(Context ctx, long id) {
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.switchIfEmpty(Mono.error(new CommandFailedException("This ID is already not blacklisted")))
				.flatMap(ctx.getBot().getDatabase()::delete)
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().unblacklist(id)))
				.then(ctx.reply("**" + id + "** is no longer blacklisted!")
						.and(ctx.getBot().log("ID removed from blacklist: " + id)));
	}
}
