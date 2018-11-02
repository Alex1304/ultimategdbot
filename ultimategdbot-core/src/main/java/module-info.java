module ultimategdbot.core {
	requires reactor.core;
	requires transitive discord4j.core.b27dd7d;
	requires org.reactivestreams;
	requires jcl.core;
	requires ultimategdbot.plugin.api;
	requires ultimategdbot.utils;
	requires ultimategdbot.database;
	
	uses com.github.alex1304.ultimategdbot.plugin.api.Command;
	uses com.github.alex1304.ultimategdbot.plugin.api.Service;
}