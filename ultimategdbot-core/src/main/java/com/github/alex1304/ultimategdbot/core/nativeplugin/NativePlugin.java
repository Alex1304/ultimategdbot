package com.github.alex1304.ultimategdbot.core.nativeplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.Command;
import com.github.alex1304.ultimategdbot.api.Plugin;
import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;
import com.github.alex1304.ultimategdbot.api.guildsettings.NativeGuildSettings;
import com.github.alex1304.ultimategdbot.api.utils.GuildSettingsValueConverter;
import com.github.alex1304.ultimategdbot.api.utils.PropertyParser;

public class NativePlugin implements Plugin {
	
	private Bot bot;

	@Override
	public void setup(Bot bot, PropertyParser parser) {
		this.bot = bot;
	}

	@Override
	public void onBotReady() {
		return;
	}
	
	@Override
	public Set<Command> getProvidedCommands() {
		return Set.of(new HelpCommand(), new PingCommand(), new SetupCommand(), new SystemCommand());
	}

	@Override
	public String getName() {
		return "Core";
	}

	@Override
	public Set<String> getDatabaseMappingResources() {
		return Set.of("/NativeGuildSettings.hbm.xml");
	}

	@Override
	public Map<String, GuildSettingsEntry<?, ?>> getGuildConfigurationEntries() {
		var map = new HashMap<String, GuildSettingsEntry<?, ?>>();
		var valueConverter = new GuildSettingsValueConverter(bot);
		map.put("prefix", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getPrefix,
				NativeGuildSettings::setPrefix,
				(value, guildId) -> valueConverter.justCheck(value, guildId, x -> !x.isBlank(), "Cannot be blank"),
				valueConverter::noConversion
		));
		map.put("server_mod_role", new GuildSettingsEntry<>(
				NativeGuildSettings.class,
				NativeGuildSettings::getServerModRoleId,
				NativeGuildSettings::setServerModRoleId,
				valueConverter::toRoleId,
				valueConverter::fromRoleId
		));
		return map;
	}
}
