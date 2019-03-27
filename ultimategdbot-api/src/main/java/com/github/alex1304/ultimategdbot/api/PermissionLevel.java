package com.github.alex1304.ultimategdbot.api;

import java.util.function.Function;

import com.github.alex1304.ultimategdbot.api.database.BotAdmins;
import com.github.alex1304.ultimategdbot.api.database.NativeGuildSettings;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

/**
 * Permission level of a bot command.
 */
@FunctionalInterface
public interface PermissionLevel {
	
	static final PermissionLevel BOT_OWNER = ctx -> ctx.getBot().getDiscordClients()
			.flatMap(DiscordClient::getApplicationInfo)
			.next()
			.flatMap(ApplicationInfo::getOwner)
			.map(ctx.getEvent().getMessage().getAuthor().get()::equals)
			.defaultIfEmpty(false).onErrorReturn(false);
	
	static final PermissionLevel BOT_ADMIN = ctx -> BOT_OWNER.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getBot().getDatabase()
					.findByID(BotAdmins.class, ctx.getEvent().getMessage().getAuthor().get().getId().asLong())
					.hasElement()
					.onErrorReturn(false));
	
	static final PermissionLevel SERVER_ADMIN = ctx -> BOT_ADMIN.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getEvent().getMessage().getChannel()
					.ofType(GuildChannel.class)
					.flatMap(c -> c.getEffectivePermissions(ctx.getEvent().getMessage().getAuthor().get().getId())
					.map(ps -> ps.contains(Permission.ADMINISTRATOR)))
					.defaultIfEmpty(false).onErrorReturn(false));
	
	static final PermissionLevel SERVER_MOD = ctx -> SERVER_ADMIN.isGranted(ctx)
			.flatMap(isGranted -> isGranted ? Mono.just(true) : ctx.getEvent().getGuildId().isEmpty()
					? Mono.just(false)
					: ctx.getBot().getDatabase()
							.findByID(NativeGuildSettings.class, ctx.getEvent().getGuildId().get().asLong())
							.map(NativeGuildSettings::getServerModRoleId)
							.map(Snowflake::of)
							.map(id -> ctx.getEvent().getMember().map(m -> m.getRoleIds().contains(id)).orElse(false))
							.defaultIfEmpty(false).onErrorReturn(false));
	
	static final PermissionLevel PUBLIC = ctx -> Mono.just(true);
	
	static PermissionLevel forSpecificRole(Function<Context, Mono<Snowflake>> roleIdGetter) {
		return ctx -> roleIdGetter.apply(ctx)
				.flatMap(roleId -> ctx.getEvent().getMember()
						.map(m -> Mono.just(m.getRoleIds().contains(roleId)))
						.orElse(Mono.just(false)));
	}
	
	/**
	 * Whether the user is granted the permission to use the command.
	 * 
	 * @param ctx - the context of the command
	 * @return a Mono emitting true if granted, false if not
	 */
	Mono<Boolean> isGranted(Context ctx);
}
