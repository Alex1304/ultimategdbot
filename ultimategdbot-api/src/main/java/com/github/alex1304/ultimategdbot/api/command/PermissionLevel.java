package com.github.alex1304.ultimategdbot.api.command;

import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;

import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Permission level of a bot command.
 */
public enum PermissionLevel {
	
	BOT_OWNER(ctx -> ctx.getBot().getApplicationInfo()
			.flatMap(ApplicationInfo::getOwner)
			.map(ctx.getEvent().getMessage().getAuthor().get()::equals)
			.defaultIfEmpty(false).onErrorReturn(false)),
	
	BOT_ADMIN(ctx -> BOT_OWNER.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getBot().getDatabase()
					.findByID(BotAdmins.class, ctx.getEvent().getMessage().getAuthor().get().getId().asLong())
					.hasElement()
					.onErrorReturn(false))),
	
	SERVER_ADMIN(ctx -> BOT_ADMIN.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getEvent().getMessage().getChannel()
					.ofType(GuildChannel.class)
					.flatMap(c -> c.getEffectivePermissions(ctx.getEvent().getMessage().getAuthor().get().getId())
					.map(ps -> ps.contains(Permission.ADMINISTRATOR)))
					.defaultIfEmpty(false).onErrorReturn(false))),
	
	SERVER_MOD(ctx -> SERVER_ADMIN.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getEvent().getGuildId().isEmpty()
					? Mono.just(false)
					: ctx.getBot().getDatabase()
							.findByID(NativeGuildSettings.class, ctx.getEvent().getGuildId().get().asLong())
							.map(NativeGuildSettings::getServerModRoleId)
							.map(Snowflake::of)
							.map(id -> ctx.getEvent().getMember().map(m -> m.getRoleIds().contains(id)).orElse(false))
							.defaultIfEmpty(false).onErrorReturn(false))),
	
	PUBLIC(ctx -> Mono.just(true));
	
	private final Function<Context, Mono<Boolean>> isGranted;
	
	private PermissionLevel(Function<Context, Mono<Boolean>> isGranted) {
		this.isGranted = isGranted;
	}
	
	public Mono<Boolean> isGranted(Context ctx) {
		return isGranted.apply(ctx);
	}
}
