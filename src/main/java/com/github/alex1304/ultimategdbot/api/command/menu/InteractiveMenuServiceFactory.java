package com.github.alex1304.ultimategdbot.api.command.menu;

import java.time.Duration;
import java.util.Set;

import com.github.alex1304.ultimategdbot.api.command.CommandServiceFactory;
import com.github.alex1304.ultimategdbot.api.emoji.EmojiServiceFactory;
import com.github.alex1304.ultimategdbot.api.service.ServiceFactory;
import com.github.alex1304.ultimategdbot.api.util.PropertyReader;

import reactor.core.publisher.Mono;

public class InteractiveMenuServiceFactory implements ServiceFactory<InteractiveMenuService> {

	@Override
	public Mono<InteractiveMenuService> create(PropertyReader properties) {
		return Mono.fromSupplier(() -> new InteractiveMenuService(
				properties.readOptional("interactive_menu.timeout_seconds")
						.map(Integer::parseInt)
						.map(Duration::ofSeconds)
						.orElse(Duration.ofMinutes(10)),
				new PaginationControls(
						properties.readOptional("interactive_menu.controls.previous").orElse(PaginationControls.DEFAULT_PREVIOUS_EMOJI),
						properties.readOptional("interactive_menu.controls.next").orElse(PaginationControls.DEFAULT_NEXT_EMOJI),
						properties.readOptional("interactive_menu.controls.close").orElse(PaginationControls.DEFAULT_CLOSE_EMOJI))));
	}

	@Override
	public Class<InteractiveMenuService> type() {
		return InteractiveMenuService.class;
	}

	@Override
	public Set<ServiceFactory<?>> requiredServices() {
		return Set.of(new CommandServiceFactory(), new EmojiServiceFactory());
	}
}
