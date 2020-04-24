module ultimategdbot.api {
	exports com.github.alex1304.ultimategdbot.api;
	exports com.github.alex1304.ultimategdbot.api.util;
	exports com.github.alex1304.ultimategdbot.api.database;
	exports com.github.alex1304.ultimategdbot.api.command.annotated.paramconverter;
	exports com.github.alex1304.ultimategdbot.api.util.menu;
	exports com.github.alex1304.ultimategdbot.api.command.annotated;
	exports com.github.alex1304.ultimategdbot.api.guildconfig;
	exports com.github.alex1304.ultimategdbot.api.command;

	opens com.github.alex1304.ultimategdbot.api.database;
	opens com.github.alex1304.ultimategdbot.api.guildconfig;
	
	requires io.netty.codec.http;
	requires reactor.extra;

	requires transitive discord.json;
	requires transitive discord4j.common;
	requires transitive discord4j.core;
	requires transitive discord4j.gateway;
	requires transitive discord4j.rest;
	requires transitive discord4j.voice;
	requires transitive java.logging;
	requires transitive java.sql;
	requires transitive org.jdbi.v3.core;
	requires transitive org.jdbi.v3.sqlobject;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;
	requires transitive reactor.netty;
	requires transitive stores.api;
}