package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Channel timely levels guild setting
 *
 * @author Alex1304
 */
public class ChannelGDModeratorsSetting extends ChannelGuildSetting {

	public ChannelGDModeratorsSetting(GuildSettings gs) {
		super(gs, GuildSettings::getChannelGDModerators, (g, v) -> g.setChannelGDModerators(v));
	}

}
