package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.CommandFailedException;
import com.github.alex1304.ultimategdbot.api.Context;
import com.github.alex1304.ultimategdbot.api.PermissionLevel;
import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.utils.ArgUtils;
import com.github.alex1304.ultimategdbot.api.utils.BotUtils;

import discord4j.core.object.entity.Channel.Type;
import reactor.core.publisher.Mono;

public class BotAdminsGrantCommand implements Command {

	@Override
	public Mono<Void> execute(Context ctx) {
		ArgUtils.requireMinimumArgCount(ctx, 2);
		return BotUtils.convertStringToUser(ctx.getBot(), ctx.getArgs().get(1))
				.flatMap(user -> ctx.getBot().getDatabase().findByID(BotAdmins.class, user.getId().asLong())
						.flatMap(__ -> Mono.error(new CommandFailedException("This user is already an admin.")))
						.then(Mono.just(new BotAdmins())
								.doOnNext(newAdmin -> newAdmin.setUserId(user.getId().asLong()))
								.flatMap(ctx.getBot().getDatabase()::save))
						.then(ctx.reply("**" + BotUtils.formatDiscordUsername(user) + "** is now a bot administrator!")))
				.then();
	}

	@Override
	public Set<String> getAliases() {
		return Set.of("grant");
	}

	@Override
	public Set<Command> getSubcommands() {
		return Set.of();
	}

	@Override
	public String getDescription() {
		return "Grants bot admin privileges to a user.";
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
	public EnumSet<Type> getChannelTypesAllowed() {
		return EnumSet.of(Type.GUILD_TEXT, Type.DM);
	}

	@Override
	public Map<Class<? extends Throwable>, BiConsumer<Throwable, Context>> getErrorActions() {
		return Map.of();
	}

}
