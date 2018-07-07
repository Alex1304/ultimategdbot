package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Role awarded levels guild setting
 *
 * @author Alex1304
 */
public class RoleTimelyLevelsSetting extends RoleGuildSetting {

	public RoleTimelyLevelsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getRoleTimelyLevels, (g, v) -> g.setRoleTimelyLevels(v));
	}
}
