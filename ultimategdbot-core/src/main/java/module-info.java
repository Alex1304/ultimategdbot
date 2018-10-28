module ultimategdbot.core {
	requires reactor.core;
	requires transitive discord4j.core.b27dd7d;
	requires org.reactivestreams;
	requires jcl.core;
	requires ultimategdbot.command.api;
	requires ultimategdbot.utils;
	
	uses com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
}