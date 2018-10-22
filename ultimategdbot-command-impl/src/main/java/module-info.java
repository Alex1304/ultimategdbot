module ultimategdbot.command.impl {
	requires discord4j.core.b27dd7d;
	requires discord4j.rest.b27dd7d;
	requires ultimategdbot.command.api;
	requires ultimategdbot.logic;
	
	provides com.github.alex1304.ultimategdbot.command.api.DiscordCommand with
		com.github.alex1304.ultimategdbot.command.impl.PingCommand,
		com.github.alex1304.ultimategdbot.command.impl.PongCommand;
}