package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.RequestBuffer;

/**
 * Represents a guild setting which value is a Discord channel
 *
 * @author Alex1304
 */
public abstract class ChannelGuildSetting extends GuildSetting<Long, IChannel> {

	public ChannelGuildSetting(GuildSettings gs, Function<GuildSettings, Long> valueGetter, BiConsumer<GuildSettings, Long> valueSetter) {
		super(gs, valueGetter, valueSetter);
	}

	@Override
	protected IChannel buildValue() {
		return RequestBuffer.request(() -> gs.getGuildInstance().getChannelByID(valueGetter.apply(gs))).get();
	}

	@Override
	protected IChannel parseValue(String str) {
		IChannel v = BotUtils.stringToChannel(str, gs.getGuildInstance());
		if (v == null)
			throw new IllegalArgumentException();
		return v;
	}
	
	@Override
	public Long valueToDatabaseType(IChannel value) {
		return value != null ? value.getLongID() : 0;
	}
	
	@Override
	public String toString() {
		IChannel v = getValue();
		if (v == null)
			return "N/A";
		else
			return v.mention();
	}

}
