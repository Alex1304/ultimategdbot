package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Role awarded levels guild setting
 *
 * @author Alex1304
 */
public class RoleAwardedLevelsSetting extends RoleGuildSetting {

	public RoleAwardedLevelsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getRoleAwardedLevels, (g, v) -> g.setRoleAwardedLevels(v));
	}
}
