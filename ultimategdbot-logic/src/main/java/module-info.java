module ultimategdbot.logic {
	requires org.reactivestreams;
	requires transitive discord4j.core.b27dd7d;
	requires transitive discord4j.rest.b27dd7d;
	requires reactor.core;
	requires jcl.core;
	requires transitive ultimategdbot.command.api;
	
	exports com.github.alex1304.ultimategdbot.logic;

	uses com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
}