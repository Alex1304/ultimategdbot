package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.InvalidSyntaxException;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class BotAdminsRevokeCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		if (ctx.getArgs().size() == 1) {
			return Mono.error(new InvalidSyntaxException(this));
		}
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
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "Revokes bot admin privileges from a user.";
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
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT, Type.DM);
	}

	@Override
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Map.of();
	}

}
