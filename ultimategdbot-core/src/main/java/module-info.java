module ultimategdbot.core {
	requires transitive ultimategdbot.api;
	requires org.reactivestreams;
	requires reactor.core;
	requires transitive org.hibernate.orm.core;
	requires java.persistence;
	requires java.naming;
	requires java.sql;
	requires discord4j.rest;
	requires org.hibernate.orm.ehcache;
	requires io.netty.codec.http;
	
	exports com.github.alex1304.ultimategdbot.core.impl to ultimategdbot.core;
	exports com.github.alex1304.ultimategdbot.core.nativeplugin to ultimategdbot.core;

	provides com.github.alex1304.ultimategdbot.api.Plugin with com.github.alex1304.ultimategdbot.core.nativeplugin.NativePlugin;
	
	uses com.github.alex1304.ultimategdbot.api.Plugin;
}