package com.github.alex1304.ultimategdbot.utils;

import java.util.EnumSet;

import com.github.alex1304.ultimategdbot.core.UltimateGDBot;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

/**
 * Represents the different role levels for bot commands.
 * Role hierarchy is also defined here.
 * 
 * @author Alex1304
 *
 */
public enum BotRoles {
	SUPERADMIN((user, channel) -> user.getLongID() == UltimateGDBot.owner().getLongID()),
	MODERATOR((user, channel) -> user.getRolesForGuild(UltimateGDBot.officialGuild())
			.contains(UltimateGDBot.moderatorRole())),
	SERVER_ADMIN((user, channel) -> PermissionUtils.hasPermissions(channel, user, Permissions.ADMINISTRATOR)),
	USER((user, channel) -> true);
	
	/**
	 * Defines extended roles for each role.
	 */
	static {
		SUPERADMIN.setExtendedRoles(EnumSet.of(MODERATOR, SERVER_ADMIN, USER));
		MODERATOR.setExtendedRoles(EnumSet.of(SERVER_ADMIN, USER));
		SERVER_ADMIN.setExtendedRoles(EnumSet.of(USER));
		USER.setExtendedRoles(EnumSet.noneOf(BotRoles.class));
	}
	
	/**
	 * Predicate that determines whether a user is granted to this role
	 */
	private PredicateUserChannel conditionForUserToBeGranted;
	
	/**
	 * Constructs the enum item with the predicate provided.
	 * 
	 * @param conditionForUserToBeGranted
	 *            - predicate that determines whether a user is granted to this role
	 */
	private BotRoles(PredicateUserChannel conditionForUserToBeGranted) {
		this.conditionForUserToBeGranted = conditionForUserToBeGranted;
	}
	
	/**
	 * The set of roles that this role extends.
	 */
	private EnumSet<BotRoles> extendedRoles;
	
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
		this.extendedRoles = extendedRoles;
	}
	
	/**
	 * Returns the set of all roles which the user is granted in the specified channel.
	 * 
	 * @param user - The user to get roles from
	 * @param channel - The specific channel where the "is granted" predicate is tested
	 * @return EnumSet of roles the user is granted 
	 */
	public static EnumSet<BotRoles> botRolesForUserInChannel(IUser user, IChannel channel) {
		EnumSet<BotRoles> botRoles = EnumSet.noneOf(BotRoles.class);
		
		for (BotRoles role : values())
			if (!botRoles.contains(role) && role.conditionForUserToBeGranted.test(user, channel)) {
				botRoles.add(role);
				botRoles.addAll(role.getExtendedRoles());
			}
		
		return botRoles;
	}
	
	public static boolean isGranted(IUser user, IChannel channel, BotRoles role) {
		return botRolesForUserInChannel(user, channel).contains(role);
	}
	
	public static boolean isGrantedAll(IUser user, IChannel channel, EnumSet<BotRoles> setOfRoles) {
		for (BotRoles role : setOfRoles)
			if (!isGranted(user, channel, role))
				return false;
		return true;
	}
	
	public static BotRoles getHighestBotRoleInSet(EnumSet<BotRoles> set) {
		if (set.isEmpty())
			return null;
		
		int lowestOrdinal = values().length;
		
		for (BotRoles br : set)
			if (br.ordinal() < lowestOrdinal)
				lowestOrdinal = br.ordinal();
		
		return values()[lowestOrdinal];
	}

	public PredicateUserChannel getConditionForUserToBeGranted() {
		return conditionForUserToBeGranted;
	}
	
	interface PredicateUserChannel {
		boolean test(IUser user, IChannel channel);
	}
}
