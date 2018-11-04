package com.github.alex1304.ultimategdbot.plugin.api;

import java.util.EnumSet;
import java.util.Objects;

import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
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
	OWNER((bot, user, channel) -> user.flatMap(u -> bot.getOwner().map(o -> u.equals(o)))),
	MODERATOR((bot, user,
			channel) -> user.flatMap(u -> bot.getOfficialGuild()
					.flatMap(og -> u.asMember(og.getId())
							.flatMap(m -> bot.getModeratorRole().flatMap(mr -> m.getRoles().hasElement(mr)))))),
	SERVER_ADMIN((bot, user,
			channel) -> channel.flatMap(c -> user.flatMap(
					u -> c.getEffectivePermissions(u.getId()).map(ps -> ps.contains(Permission.MANAGE_GUILD))))),
	USER((bot, user, channel) -> Mono.just(true));

	/**
	 * Defines extended roles for each role.
	 */
	static {
		OWNER.setExtendedRoles(EnumSet.of(MODERATOR, SERVER_ADMIN, USER));
		MODERATOR.setExtendedRoles(EnumSet.of(SERVER_ADMIN, USER));
		SERVER_ADMIN.setExtendedRoles(EnumSet.of(USER));
		USER.setExtendedRoles(EnumSet.noneOf(BotRoles.class));
	}

	/**
	 * Predicate that determines whether a user is granted to this role
	 */
	private Function3<UltimateGDBot, Mono<User>, Mono<GuildChannel>, Mono<Boolean>> conditionForUserToBeGranted;

	/**
	 * The set of roles that this role extends.
	 */
	private EnumSet<BotRoles> extendedRoles;

	private BotRoles(
			Function3<UltimateGDBot, Mono<User>, Mono<GuildChannel>, Mono<Boolean>> conditionForUserToBeGranted) {
		this.conditionForUserToBeGranted = conditionForUserToBeGranted;
	}

	/**
	 * Gets the extended roles
	 * 
	 * @return EnumSet of roles
	 */
	public EnumSet<BotRoles> getExtendedRoles() {
		return extendedRoles;
	}

	/**
	 * Sets the extended roles
	 * 
	 * @param EnumSet of roles
	 */
	public void setExtendedRoles(EnumSet<BotRoles> extendedRoles) {
		this.extendedRoles = Objects.requireNonNull(extendedRoles);
	}

	public static Mono<Boolean> isGranted(UltimateGDBot bot, Mono<User> user, Mono<GuildChannel> channel,
			BotRoles role) {
		return Objects.requireNonNull(role).conditionForUserToBeGranted.apply(Objects.requireNonNull(bot),
				Objects.requireNonNull(user), Objects.requireNonNull(channel));
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