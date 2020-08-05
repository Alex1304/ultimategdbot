package com.github.alex1304.ultimategdbot.api.service;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.CommandService;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public final class RootServiceSetupHelper<S> {
	
	private final Supplier<S> constructor;
	private final List<CommandProviderWithFilter> commandProviders = new ArrayList<>();
	private Publisher<?> setupSequence;
	
	private RootServiceSetupHelper(Supplier<S> constructor) {
		this.constructor = constructor;
	}

	public static <S> RootServiceSetupHelper<S> create(Supplier<S> constructor) {
		requireNonNull(constructor);
		return new RootServiceSetupHelper<>(constructor);
	}
	
	public RootServiceSetupHelper<S> setSetupSequence(@Nullable Publisher<?> setupSequence) {
		this.setupSequence = setupSequence;
		return this;
	}

	public RootServiceSetupHelper<S> addCommandProvider(CommandService commandService, CommandProvider commandProvider,
			@Nullable Predicate<String> classNameFilter) {
		requireNonNull(commandService);
		requireNonNull(commandProvider);
		commandService.addProvider(commandProvider);
		commandProviders.add(new CommandProviderWithFilter(commandProvider, classNameFilter));
		return this;
	}

	public RootServiceSetupHelper<S> addCommandProvider(CommandService commandService, CommandProvider commandProvider) {
		return addCommandProvider(commandService, commandProvider, null);
	}
	
	public Mono<S> setup() {
		return Mono.fromSupplier(constructor)
				.flatMap(service -> {
					var setupSequence = this.setupSequence != null
							? this.setupSequence: Mono.empty();
					var commandProviders = this.commandProviders != null
							? this.commandProviders : List.<CommandProviderWithFilter>of();
					var loadingCommands = Flux.fromIterable(commandProviders)
							.flatMap(commandProviderWithFilter -> commandProviderWithFilter.commandProvider
									.addAllFromModule(service.getClass().getModule(), commandProviderWithFilter.classNameFilter)
									.doOnNext(obj -> {
										for (Field f : obj.getClass().getDeclaredFields()) {
											f.setAccessible(true);
											if (f.isAnnotationPresent(Root.class)) {
												if (!f.getType().isInstance(service)) {
													throw new ClassCastException("The field declared @Root does not have a "
															+ "compatible type (cannot cast " + service.getClass().getName()
															+ " to " + f.getType().getName() + ")");
												}
												try {
													f.set(obj, service);
												} catch (IllegalAccessException e) {
													throw Exceptions.propagate(e);
												}
											}
										}
									}));
					return Mono.when(setupSequence, loadingCommands).thenReturn(service);
				});
	}
	
	private static class CommandProviderWithFilter {
		
		private final CommandProvider commandProvider;
		private final Predicate<String> classNameFilter;
		
		private CommandProviderWithFilter(CommandProvider commandProvider, Predicate<String> classNameFilter) {
			this.commandProvider = commandProvider;
			this.classNameFilter = classNameFilter;
		}
	}
}
