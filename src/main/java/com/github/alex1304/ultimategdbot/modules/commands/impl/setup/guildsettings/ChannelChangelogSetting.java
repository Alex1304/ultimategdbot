package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

/**
 * Channel changelog guild setting
 *
 * @author Alex1304
 */
public class ChannelChangelogSetting extends ChannelGuildSetting {

	public ChannelChangelogSetting(GuildSettings gs) {
		super(gs, GuildSettings::getChannelChangelog, (g, v) -> g.setChannelChangelog(v));
	}

}
