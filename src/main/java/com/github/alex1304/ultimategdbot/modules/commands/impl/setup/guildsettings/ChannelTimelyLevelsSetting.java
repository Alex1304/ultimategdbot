package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Channel timely levels guild setting
 *
 * @author Alex1304
 */
public class ChannelTimelyLevelsSetting extends ChannelGuildSetting {

	public ChannelTimelyLevelsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getChannelTimelyLevels, (g, v) -> g.setChannelTimelyLevels(v));
	}

}
