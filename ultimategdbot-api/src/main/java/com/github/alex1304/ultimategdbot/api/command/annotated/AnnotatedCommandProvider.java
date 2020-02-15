package com.github.alex1304.ultimategdbot.api.command.annotated;

import java.util.HashMap;
import java.util.Map;

import com.github.alex1304.ultimategdbot.api.command.CommandProvider;
import com.github.alex1304.ultimategdbot.api.command.Context;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.GuildChannelConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.IntConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.LongConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.ParamConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.RoleConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.StringConverter;
import com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter.UserConverter;

import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import reactor.core.publisher.Mono;

public class AnnotatedCommandProvider extends CommandProvider {
	
	private final Map<Class<?>, ParamConverter<?>> paramConverters = initDefaultConverters();
	
	/**
	 * Adds a new param converter to this annotated command provider.
	 * 
	 * @param converter the converter to add
	 */
	public void addParamConverter(ParamConverter<?> converter) {
		paramConverters.put(converter.type(), converter);
	}
	
	/**
	 * Builds and adds a new command based on the given annotated object.
	 * 
	 * @param annotated the annotated object to add
	 * @throws InvalidAnnotatedObjectException if the annotated object is malformed.
	 */
	public void addAnnotated(Object annotated) {
		add(AnnotatedCommand.fromAnnotatedObject(annotated, this));
	}
	
	@SuppressWarnings("unchecked")
	<T> Mono<T> convert(Context ctx, String input, Class<T> targetType) {
		var converter = (ParamConverter<T>) paramConverters.get(targetType);
		if (converter == null) {
			return Mono.error(new RuntimeException("No param converter available for the type " + targetType.getName()));
		}
		return converter.convert(ctx, input);
	}
	
	private static Map<Class<?>, ParamConverter<?>> initDefaultConverters() {
		var map = new HashMap<Class<?>, ParamConverter<?>>();
		map.put(String.class, new StringConverter());
		map.put(Integer.class, new IntConverter());
		map.put(int.class, new IntConverter());
		map.put(Long.class, new LongConverter());
		map.put(long.class, new LongConverter());
		map.put(Role.class, new RoleConverter());
		map.put(User.class, new UserConverter());
		map.put(GuildChannel.class, new GuildChannelConverter());
		return map;
	}
}
