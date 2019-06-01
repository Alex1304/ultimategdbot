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

class BlacklistAddCommand implements Command {

	private final NativePlugin plugin;
	
	public BlacklistAddCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		var id = ArgUtils.getArgAsLong(ctx, 1);
		return ctx.getBot().getDatabase().findByID(BlacklistedIds.class, id)
				.flatMap(__ -> Mono.error(new CommandFailedException("This ID is already blacklisted")))
				.then(Mono.fromCallable(() -> {
							var b = new BlacklistedIds();
							b.setId(id);
							return b;
						}).flatMap(ctx.getBot().getDatabase()::save))
				.then(Mono.fromRunnable(() -> ctx.getBot().getCommandKernel().blacklist(id)))
				.then(ctx.reply("**" + id + "** is now blacklisted!"))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("add");
	}

	@Override
	public String getDescription() {
		return "Adds a user ID, a channel ID or a guild ID to the blacklist.";
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
