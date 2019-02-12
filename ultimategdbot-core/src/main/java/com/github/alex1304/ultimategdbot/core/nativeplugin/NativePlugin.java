package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;

public class NativePlugin implements Plugin {

	@Override
	public Set<Command> getProvidedCommands() {
		return Set.of(new SetupCommand());
	}

	@Override
	public String getName() {
		return "Native";
	}

	@Override
	public Set<String> getDatabaseMappingResources() {
		return Set.of("NativeGuildSettings.hbm.xml");
	}

	@Override
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries(Bot bot) {
		var map = new HashMap<String, GuildSettingsEntry<?, ?>>();
		map.put("prefix", new GuildSettingsEntry<>(NativeGuildSettings.class, NativeGuildSettings::getPrefix, NativeGuildSettings::setPrefix, String::toString, String::toString));
		map.put("server_mod_role", new GuildSettingsEntry<>(NativeGuildSettings.class, NativeGuildSettings::getServerModRoleId, NativeGuildSettings::setServerModRoleId, str -> Long.parseLong(str.substring(3, str.length() - 1)), String::valueOf));
		return map;
	}

}
