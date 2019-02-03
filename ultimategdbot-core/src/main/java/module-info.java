module ultimategdbot.core {
	requires transitive ultimategdbot.api;
	requires org.reactivestreams;
	requires reactor.core;
	requires transitive org.hibernate.orm.core;
	requires java.persistence;
	requires java.naming;
	requires java.sql;
	
	exports com.github.alex1304.ultimategdbot.core.impl;
	exports com.github.alex1304.ultimategdbot.core.handler;
	
	uses com.github.alex1304.ultimategdbot.api.Plugin;
}