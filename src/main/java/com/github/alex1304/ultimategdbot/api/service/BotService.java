package com.github.alex1304.ultimategdbot.api.service;

import com.github.alex1304.ultimategdbot.api.command.CommandService;
import com.github.alex1304.ultimategdbot.api.command.menu.InteractiveMenuService;
import com.github.alex1304.ultimategdbot.api.database.DatabaseService;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiService;
import com.github.alex1304.ultimategdbot.api.localization.LocalizationService;
import com.github.alex1304.ultimategdbot.api.logging.LoggingService;
import com.github.alex1304.ultimategdbot.api.metadata.PluginMetadataService;

import discord4j.core.GatewayDiscordClient;

public final class BotService {
	
	private final GatewayDiscordClient gateway;
	private final CommandService commandService;
	private final DatabaseService databaseService;
	private final EmojiService emojiService;
	private final InteractiveMenuService interactiveMenuService;
	private final LocalizationService localizationService;
	private final LoggingService loggingService;
	private final PluginMetadataService pluginMetadataService;

	public BotService(
			GatewayDiscordClient gateway,
			CommandService commandService,
			DatabaseService databaseService,
			EmojiService emojiService,
			InteractiveMenuService interactiveMenuService,
			LocalizationService localizationService,
			LoggingService loggingService,
			PluginMetadataService pluginMetadataService) {
		this.gateway = gateway;
		this.commandService = commandService;
		this.databaseService = databaseService;
		this.emojiService = emojiService;
		this.interactiveMenuService = interactiveMenuService;
		this.localizationService = localizationService;
		this.loggingService = loggingService;
		this.pluginMetadataService = pluginMetadataService;
	}

	public GatewayDiscordClient gateway() {
		return gateway;
	}

	public CommandService command() {
		return commandService;
	}

	public DatabaseService database() {
		return databaseService;
	}
	
	public EmojiService emoji() {
		return emojiService;
	}

	public InteractiveMenuService interactiveMenu() {
		return interactiveMenuService;
	}

	public LocalizationService localization() {
		return localizationService;
	}

	public LoggingService logging() {
		return loggingService;
	}

	public PluginMetadataService pluginMetadata() {
		return pluginMetadataService;
	}
}
