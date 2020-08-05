import com.github.alex1304.ultimategdbot.api.service.CommonServices;
import com.github.alex1304.ultimategdbot.api.service.ServiceDeclarator;

module ultimategdbot.api {
	exports com.github.alex1304.ultimategdbot.api;
	exports com.github.alex1304.ultimategdbot.api.command;
	exports com.github.alex1304.ultimategdbot.api.command.annotated;
	exports com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;
	exports com.github.alex1304.ultimategdbot.api.command.menu;
	exports com.github.alex1304.ultimategdbot.api.database;
	exports com.github.alex1304.ultimategdbot.api.database.guildconfig;
	exports com.github.alex1304.ultimategdbot.api.emoji;
	exports com.github.alex1304.ultimategdbot.api.localization;
	exports com.github.alex1304.ultimategdbot.api.logging;
	exports com.github.alex1304.ultimategdbot.api.metadata;
	exports com.github.alex1304.ultimategdbot.api.service;
	exports com.github.alex1304.ultimategdbot.api.util;

	opens com.github.alex1304.ultimategdbot.api.database;
	opens com.github.alex1304.ultimategdbot.api.database.guildconfig;
	
	provides ServiceDeclarator with CommonServices;
	
	requires io.netty.codec.http;
	requires reactor.extra;

	requires transitive discord4j.common;
	requires transitive discord4j.core;
	requires transitive discord4j.discordjson;
	requires transitive discord4j.discordjson.api;
	requires transitive discord4j.gateway;
	requires transitive discord4j.rest;
	requires transitive discord4j.store.api;
	requires transitive discord4j.voice;
	requires transitive java.logging;
	requires transitive java.sql;
	requires transitive org.jdbi.v3.core;
	requires transitive org.jdbi.v3.sqlobject;
	requires transitive org.reactivestreams;
	requires transitive rdi;
	requires transitive reactor.core;
	requires transitive reactor.netty;
}