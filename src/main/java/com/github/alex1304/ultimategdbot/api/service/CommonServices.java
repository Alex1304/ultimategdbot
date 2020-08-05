package com.github.alex1304.ultimategdbot.api.service;

import static com.github.alex1304.rdi.config.FactoryMethod.constructor;
import static com.github.alex1304.rdi.config.Injectable.ref;
import static com.github.alex1304.rdi.config.Injectable.value;

import java.util.Set;

import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.ultimategdbot.api.BotConfig;
import com.github.alex1304.ultimategdbot.api.command.CommandService;
import com.github.alex1304.ultimategdbot.api.command.menu.InteractiveMenuService;
import com.github.alex1304.ultimategdbot.api.database.DatabaseService;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiService;
import com.github.alex1304.ultimategdbot.api.localization.LocalizationService;
import com.github.alex1304.ultimategdbot.api.logging.LoggingService;
import com.github.alex1304.ultimategdbot.api.metadata.PluginMetadataService;

import discord4j.core.GatewayDiscordClient;

/**
 * Defines references and descriptors for all common services.
 */
public final class CommonServices implements ServiceDeclarator {
	
	public static final ServiceReference<BotService> BOT = ServiceReference.ofType(BotService.class);
	public static final ServiceReference<GatewayDiscordClient> GATEWAY_DISCORD_CLIENT = ServiceReference.ofType(GatewayDiscordClient.class);
	public static final ServiceReference<CommandService> COMMAND_SERVICE = ServiceReference.ofType(CommandService.class);
	public static final ServiceReference<DatabaseService> DATABASE_SERVICE = ServiceReference.ofType(DatabaseService.class);
	public static final ServiceReference<EmojiService> EMOJI_SERVICE = ServiceReference.ofType(EmojiService.class);
	public static final ServiceReference<InteractiveMenuService> INTERACTIVE_MENU_SERVICE = ServiceReference.ofType(InteractiveMenuService.class);
	public static final ServiceReference<LocalizationService> LOCALIZATION_SERVICE = ServiceReference.ofType(LocalizationService.class);
	public static final ServiceReference<LoggingService> LOGGING_SERVICE = ServiceReference.ofType(LoggingService.class);
	public static final ServiceReference<PluginMetadataService> PLUGIN_METADATA_SERVICE = ServiceReference.ofType(PluginMetadataService.class);

	@Override
	public Set<ServiceDescriptor> declareServices(BotConfig botConfig) {
		return Set.of(
				ServiceDescriptor.builder(BOT)
						.setFactoryMethod(constructor(
								ref(GATEWAY_DISCORD_CLIENT),
								ref(COMMAND_SERVICE),
								ref(DATABASE_SERVICE),
								ref(EMOJI_SERVICE),
								ref(INTERACTIVE_MENU_SERVICE),
								ref(LOCALIZATION_SERVICE),
								ref(LOGGING_SERVICE),
								ref(PLUGIN_METADATA_SERVICE)
						))
						.build(),
				ServiceDescriptor.builder(COMMAND_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(GATEWAY_DISCORD_CLIENT),
								ref(LOCALIZATION_SERVICE),
								ref(LOGGING_SERVICE)))
						.build(),
				ServiceDescriptor.builder(EMOJI_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(GATEWAY_DISCORD_CLIENT)))
						.build(),
				ServiceDescriptor.builder(INTERACTIVE_MENU_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(GATEWAY_DISCORD_CLIENT),
								ref(COMMAND_SERVICE),
								ref(EMOJI_SERVICE)))
						.build(),
				ServiceDescriptor.builder(LOCALIZATION_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class)))
						.build(),
				ServiceDescriptor.builder(LOGGING_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(GATEWAY_DISCORD_CLIENT)))
						.build()
		);
	}

}
