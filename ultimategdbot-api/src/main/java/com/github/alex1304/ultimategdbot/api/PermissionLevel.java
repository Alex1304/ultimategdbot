package com.github.alex1304.ultimategdbot.api;

import discord4j.core.object.util.Permission;
import reactor.core.publisher.Mono;

/**
 * Permission level of a bot command.
 */
@FunctionalInterface
public interface PermissionLevel {
	
	public static final PermissionLevel BOT_OWNER = ctx -> ctx.getEvent().getMessage().getAuthor()
			.flatMap(u -> ctx.getBot().getDiscordClient().getApplicationInfo()
					.flatMap(ai -> ai.getOwner()
							.map(o -> u.equals(o))));
	
	public static final PermissionLevel BOT_MODERATOR = ctx -> ctx.getEvent().getMessage().getAuthor()
			.flatMap(u -> ctx.getBot().getSupportServer()
					.flatMap(ss -> u.asMember(ss.getId())
							.flatMap(m -> ctx.getBot().getModeratorRole()
									.flatMap(mr -> m.getRoles().hasElement(mr)))));
	
	public static final PermissionLevel SERVER_ADMIN = ctx -> ctx.getEvent().getGuild()
			.flatMap(g -> g.getChannelById(ctx.getEvent().getMessage().getChannelId())
					.flatMap(c -> ctx.getEvent().getMessage().getAuthor()
							.flatMap(u -> c.getEffectivePermissions(u.getId())
									.map(ps -> ps.contains(Permission.MANAGE_GUILD)))));
	
	public static final PermissionLevel PUBLIC = ctx -> Mono.just(true);
	
	/**
	 * Whether the user is granted the permission to use the command.
	 * 
	 * @param ctx - the context of the command
	 * @return a Mono emitting true if granted, false if not
	 */
	Mono<Boolean> isGranted(Context ctx);
}
