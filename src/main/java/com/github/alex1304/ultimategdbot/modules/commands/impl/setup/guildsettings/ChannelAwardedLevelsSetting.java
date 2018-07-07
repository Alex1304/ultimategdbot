package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Channel awarded levels guild setting
 *
 * @author Alex1304
 */
public class ChannelAwardedLevelsSetting extends ChannelGuildSetting {

	public ChannelAwardedLevelsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getChannelAwardedLevels, (g, v) -> g.setChannelAwardedLevels(v));
	}
}
