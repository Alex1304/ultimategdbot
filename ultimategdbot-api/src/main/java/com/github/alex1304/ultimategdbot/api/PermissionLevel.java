package com.github.alex1304.ultimategdbot.api;

import java.util.function.Function;

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
			.flatMap(client -> client.getApplicationInfo())
			.next()
			.filter(__ -> ctx.getEvent().getMessage().getAuthor().isPresent())
			.flatMap(ai -> ai.getOwner())
			.map(ctx.getEvent().getMessage().getAuthor().get()::equals)
			.defaultIfEmpty(false).onErrorReturn(false);
	
	static final PermissionLevel BOT_MODERATOR = ctx -> ctx.getBot().getSupportServer()
			.filter(__ -> ctx.getEvent().getMessage().getAuthor().isPresent())
			.flatMap(ss -> ctx.getEvent().getMessage().getAuthor().get().asMember(ss.getId()))
			.flatMap(m -> ctx.getBot().getModeratorRole().flatMap(mr -> m.getRoles().hasElement(mr)))
			.defaultIfEmpty(false).onErrorReturn(false);
	
	static final PermissionLevel SERVER_ADMIN = ctx -> ctx.getEvent().getMessage().getChannel()
			.filter(__ -> ctx.getEvent().getMessage().getAuthor().isPresent())
			.ofType(GuildChannel.class)
			.flatMap(c -> c.getEffectivePermissions(ctx.getEvent().getMessage().getAuthor().get().getId())
			.map(ps -> ps.contains(Permission.ADMINISTRATOR)))
			.defaultIfEmpty(false).onErrorReturn(false);
	
	static final PermissionLevel PUBLIC = ctx -> Mono.just(true);
	
	static PermissionLevel forSpecificRole(Function<Context, Snowflake> roleIdGetter) {
		return ctx -> ctx.getEvent().getMember().map(m -> Mono.just(m.getRoleIds().contains(roleIdGetter.apply(ctx)))).orElse(Mono.just(false));
	}
	
	/**
	 * Whether the user is granted the permission to use the command.
	 * 
	 * @param ctx - the context of the command
	 * @return a Mono emitting true if granted, false if not
	 */
	Mono<Boolean> isGranted(Context ctx);
}
