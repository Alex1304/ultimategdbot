package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.PluginSetupException;
import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.guildsettings.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.GuildSettingsValueConverter;

public class NativePlugin implements Plugin {

	@Override
	public Set<Command> getProvidedCommands() {
		return Set.of(new HelpCommand(), new PingCommand(), new SetupCommand(), new TestArgsCommand());
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
		var valueConverter = new GuildSettingsValueConverter(bot);
		map.put("prefix", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getPrefix,
				NativeGuildSettings::setPrefix,
				valueConverter::convertNonBlankStringToString,
				valueConverter::convertStringToString
		));
		map.put("server_mod_role", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getServerModRoleId,
				NativeGuildSettings::setServerModRoleId,
				valueConverter::convertStringToRoleID,
				valueConverter::convertRoleIDToString
		));
		return map;
	}

	@Override
	public void setup() throws PluginSetupException {
		return;
	}
}
