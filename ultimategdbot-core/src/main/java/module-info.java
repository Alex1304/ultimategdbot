module ultimategdbot.core {
	requires transitive ultimategdbot.api;
	requires reactor.core;
	requires transitive org.hibernate.orm.core;
	requires java.persistence;
	requires java.naming;
	requires java.sql;
	requires discord4j.rest;
	requires io.netty.codec.http;
	
	exports com.github.alex1304.ultimategdbot.core.impl;
	exports com.github.alex1304.ultimategdbot.core.handler;
	
	uses com.github.alex1304.ultimategdbot.api.Command;
}