package com.github.alex1304.ultimategdbot.core;

import java.util.Objects;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import reactor.core.publisher.Mono;

class BotAdminsRevokeCommand implements Command {

	private final NativePlugin plugin;
	
	public BotAdminsRevokeCommand(NativePlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		return BotUtils.convertStringToUser(ctx.getBot(), ctx.getArgs().get(1))
				.flatMap(user -> ctx.getBot().getDatabase().findByID(BotAdmins.class, user.getId().asLong())
						.switchIfEmpty(Mono.error(new CommandFailedException("This user is already not an admin.")))
						.flatMap(ctx.getBot().getDatabase()::delete)
						.then(ctx.reply("**" + BotUtils.formatDiscordUsername(user) + "** is no longer a bot administrator!")))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("revoke");
	}

	@Override
	public String getDescription() {
		return "Revokes bot admin privileges from a user.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public String getSyntax() {
		return "<discord_tag_or_ID>";
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
