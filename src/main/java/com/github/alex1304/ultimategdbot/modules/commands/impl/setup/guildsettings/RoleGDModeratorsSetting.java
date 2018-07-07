package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Role awarded levels guild setting
 *
 * @author Alex1304
 */
public class RoleGDModeratorsSetting extends RoleGuildSetting {

	public RoleGDModeratorsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getRoleGDModerators, (g, v) -> g.setRoleGDModerators(v));
	}
}
