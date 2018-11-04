module ultimategdbot.plugin.api {
	requires org.reactivestreams;
	requires reactor.core;
	requires reactor.extra;
	requires transitive discord4j.core.b27dd7d;
	requires transitive jcl.core;
	requires java.logging;
	
	exports com.github.alex1304.ultimategdbot.plugin.api;
}