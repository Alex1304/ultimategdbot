package com.github.alex1304.ultimategdbot.api;

import static com.github.alex1304.rdi.config.FactoryMethod.constructor;
import static com.github.alex1304.rdi.config.Injectable.ref;
import static com.github.alex1304.rdi.config.Injectable.value;

import java.util.Set;

import com.github.alex1304.rdi.ServiceReference;
import com.github.alex1304.rdi.config.ServiceDescriptor;
import com.github.alex1304.ultimategdbot.api.command.CommandService;
import com.github.alex1304.ultimategdbot.api.command.menu.InteractiveMenuService;
import com.github.alex1304.ultimategdbot.api.database.DatabaseService;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiService;
import com.github.alex1304.ultimategdbot.api.localization.LocalizationService;
import com.github.alex1304.ultimategdbot.api.logging.LoggingService;
import com.github.alex1304.ultimategdbot.api.metadata.PluginMetadataService;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;

/**
 * Defines references and descriptors for all common services.
 */
public final class CommonServices implements ServiceDeclarator {
	
	public static final ServiceReference<DiscordClient> DISCORD_REST_CLIENT = ServiceReference.ofType(DiscordClient.class);
	public static final ServiceReference<GatewayDiscordClient> DISCORD_GATEWAY_CLIENT = ServiceReference.ofType(GatewayDiscordClient.class);

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
				ServiceDescriptor.builder(COMMAND_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(DISCORD_GATEWAY_CLIENT),
								ref(LOCALIZATION_SERVICE),
								ref(LOGGING_SERVICE)))
						.build(),
				ServiceDescriptor.builder(EMOJI_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(DISCORD_GATEWAY_CLIENT)))
						.build(),
				ServiceDescriptor.builder(INTERACTIVE_MENU_SERVICE)
						.setFactoryMethod(constructor(
								value(botConfig, BotConfig.class),
								ref(DISCORD_GATEWAY_CLIENT),
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
								ref(DISCORD_REST_CLIENT)))
						.build()
		);
	}

}
