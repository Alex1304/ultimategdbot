package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotated.CommandSpec;
import com.github.alex1304.ultimategdbot.api.database.BlacklistedIds;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="blacklist", permLevel=PermissionLevel.BOT_OWNER)
class BlacklistCommand {

	@CommandAction("add")
	public Mono<Void> runAdd(Context ctx, long id) {
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.flatMap(__ -> Mono.error(new CommandFailedException("This ID is already blacklisted")))
				.then(Mono.fromCallable(() -> {
							var b = new BlacklistedIds();
							b.setId(id);
							return b;
						}).flatMap(ctx.getBot().getDatabase()::save))
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().blacklist(id)))
				.then(ctx.reply("**" + id + "** is now blacklisted!"))
				.then(ctx.getBot().log("ID added to blacklist: " + id))
				.then();
	}

	@CommandAction("remove")
	public Mono<Void> runRemove(Context ctx, long id) {
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.switchIfEmpty(Mono.error(new CommandFailedException("This ID is already not blacklisted")))
				.flatMap(ctx.getBot().getDatabase()::delete)
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().unblacklist(id)))
				.then(ctx.reply("**" + id + "** is no longer blacklisted!"))
				.then(ctx.getBot().log("ID removed from blacklist: " + id))
				.then();
	}
}
