package com.github.alex1304.ultimategdbot.api.command;

import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

public class CommandServiceFactory implements ServiceFactory<CommandService> {

	@Override
	public Mono<CommandService> create(PropertyReader properties) {
		return Mono.fromSupplier(() -> new CommandService(
				properties.read("command_prefix"),
				properties.read("flag_prefix"),
				properties.readOptional("debug_log_channel_id").map(Snowflake::of).orElse(null)));
	}

	@Override
	public Class<CommandService> type() {
		return CommandService.class;
	}

}
