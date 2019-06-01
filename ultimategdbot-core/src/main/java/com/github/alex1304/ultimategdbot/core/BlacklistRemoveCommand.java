package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.BlacklistedIds;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;

import reactor.core.publisher.Mono;

class BlacklistRemoveCommand implements Command {

	private final NativePlugin plugin;
	
	public BlacklistRemoveCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		var id = ArgUtils.getArgAsLong(ctx, 1);
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.switchIfEmpty(Mono.error(new CommandFailedException("This ID is already not blacklisted")))
				.flatMap(ctx.getBot().getDatabase()::delete)
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().unblacklist(id)))
				.then(ctx.reply("**" + id + "** is no longer blacklisted!"))
				.then(ctx.getBot().log("ID removed from blacklist: " + id))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("remove");
	}

	@Override
	public String getDescription() {
		return "Removes a user ID, a channel ID or a guild ID from the blacklist.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public String getSyntax() {
		return "<id>";
	}

	@Override
	public PermissionLevel getPermissionLevel() {
		return PermissionLevel.BOT_OWNER;
	}

	@Override
	public Plugin getPlugin() {
		return plugin;
	}
}
