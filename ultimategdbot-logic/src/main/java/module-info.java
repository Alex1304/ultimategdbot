module ultimategdbot.logic {
	requires org.reactivestreams;
	requires transitive discord4j.core.v3.a8b4e4e668;
	requires transitive discord4j.rest.v3.a8b4e4e668;
	requires reactor.core;
	requires transitive ultimategdbot.command.api;
	
	exports com.github.alex1304.ultimategdbot.logic;
}