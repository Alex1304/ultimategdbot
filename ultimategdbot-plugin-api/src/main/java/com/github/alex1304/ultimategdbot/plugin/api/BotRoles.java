package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.EnumSet;
import java.util.Objects;

import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

/**
 * Represents the different role levels for bot commands. Role hierarchy is also
 * defined here.
 * 
 * @author Alex1304
 *
 */
public enum BotRoles {
	OWNER((bot, user, channel) -> user.flatMap(u -> bot.getDiscordClient().getApplicationInfo().flatMap(ai -> ai.getOwner().map(o -> u.equals(o))))),
	MODERATOR((bot, user, channel) -> user.flatMap(u -> bot.getOfficialGuild()
					.flatMap(og -> u.asMember(og.getId())
							.flatMap(m -> bot.getModeratorRole().flatMap(mr -> m.getRoles().hasElement(mr)))))),
	SERVER_ADMIN((bot, user, channel) -> channel.flatMap(c -> user
			.flatMap(u -> c.getEffectivePermissions(u.getId())
					.map(ps -> ps.contains(Permission.MANAGE_GUILD))))),
	USER((bot, user, channel) -> Mono.just(true));

	/**
	 * Defines included roles for each role.
	 */
	static {
		OWNER.setIncludedRoles(EnumSet.noneOf(BotRoles.class));
		MODERATOR.setIncludedRoles(EnumSet.of(OWNER));
		SERVER_ADMIN.setIncludedRoles(EnumSet.of(OWNER, MODERATOR));
		USER.setIncludedRoles(EnumSet.of(OWNER, MODERATOR, SERVER_ADMIN));
	}

	/**
	 * Predicate that determines whether a user is granted to this role
	 */
	private Function3<Bot, Mono<User>, Mono<GuildChannel>, Mono<Boolean>> grantCondition;

	/**
	 * The set of roles that this role extends.
	 */
	private EnumSet<BotRoles> includedRoles;

	private BotRoles(Function3<Bot, Mono<User>, Mono<GuildChannel>, Mono<Boolean>> grantCondition) {
		this.grantCondition = grantCondition;
	}

	/**
	 * Sets the extended roles
	 * 
	 * @param EnumSet of roles
	 */
	public void setIncludedRoles(EnumSet<BotRoles> includedRoles) {
		this.includedRoles = Objects.requireNonNull(includedRoles);
	}

	public static Mono<Boolean> isGranted(Bot bot, Mono<User> user, Mono<GuildChannel> channel,
			BotRoles role) {
		// Null checks are done separately to improve code lisibility
		Objects.requireNonNull(role);
		Objects.requireNonNull(user);
		Objects.requireNonNull(channel);
		
		return role.grantCondition.apply(bot, user, channel)
				.flatMap(bool -> bool ? Mono.just(true) : Flux.fromIterable(role.includedRoles)
						.map(r -> r.grantCondition.apply(bot, user, channel))
						.filterWhen(isGranted -> isGranted.map(b -> !b))
						.hasElements().map(b -> !b));
	}

	public static BotRoles highestFrom(EnumSet<BotRoles> set) {
		if (Objects.requireNonNull(set).isEmpty())
			return null;

		int lowestOrdinal = values().length;

		for (BotRoles br : set)
			if (br.ordinal() < lowestOrdinal)
				lowestOrdinal = br.ordinal();

		return values()[lowestOrdinal];
	}
}