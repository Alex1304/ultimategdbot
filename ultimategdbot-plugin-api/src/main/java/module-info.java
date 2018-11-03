module ultimategdbot.plugin.api {
	requires org.reactivestreams;
	requires reactor.core;
	requires transitive discord4j.core.b27dd7d;
	requires transitive jcl.core;
	
	exports com.github.alex1304.ultimategdbot.plugin.api;
}