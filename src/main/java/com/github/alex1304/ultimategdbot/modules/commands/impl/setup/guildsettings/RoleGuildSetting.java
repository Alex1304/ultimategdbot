package com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.utils.BotUtils;

import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.RequestBuffer;

/**
 * Role guild setting
 *
 * @author Alex1304
 */
public class RoleGuildSetting extends GuildSetting<Long, IRole> {

	public RoleGuildSetting(GuildSettings gs, Function<GuildSettings, Long> valueGetter, BiConsumer<GuildSettings, Long> valueSetter) {
		super(gs, valueGetter, valueSetter);
	}

	@Override
	protected IRole buildValue() {
		return RequestBuffer.request(() -> gs.getGuildInstance().getRoleByID(valueGetter.apply(gs))).get();
	}

	@Override
	protected IRole parseValue(String str) {
		IRole v = BotUtils.stringToRole(str, gs.getGuildInstance());
		if (v == null)
			throw new IllegalArgumentException();
		return v;
	}
	
	@Override
	public Long valueToDatabaseType(IRole value) {
		return value != null ? value.getLongID() : 0;
	}
	
	@Override
	public String toString() {
		IRole v = getValue();
		if (v == null)
			return "N/A";
		else
			return v.getName();
	}
}
