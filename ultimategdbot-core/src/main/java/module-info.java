module ultimategdbot.core {
	requires reactor.core;
	requires discord4j.core.v3.a8b4e4e668;
	requires org.reactivestreams;
	requires ultimategdbot.command.api;
	requires ultimategdbot.logic;

	uses com.github.alex1304.ultimategdbot.command.api.DiscordCommand;
}