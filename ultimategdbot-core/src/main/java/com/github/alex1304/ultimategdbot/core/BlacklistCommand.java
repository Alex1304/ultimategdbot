package com.github.alex1304.ultimategdbot.core;

import com.github.alex1304.ultimategdbot.api.command.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandAction;
import com.github.alex1304.ultimategdbot.api.command.annotation.CommandSpec;
import com.github.alex1304.ultimategdbot.api.command.annotation.Subcommand;
import com.github.alex1304.ultimategdbot.api.command.parser.LongParser;
import com.github.alex1304.ultimategdbot.api.database.BlacklistedIds;

import reactor.core.publisher.Mono;

@CommandSpec(aliases="blacklist", permLevel=PermissionLevel.BOT_OWNER)
class BlacklistCommand {

	@Subcommand("add")
	@CommandAction(LongParser.class)
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

	@Subcommand("remove")
	@CommandAction(LongParser.class)
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
