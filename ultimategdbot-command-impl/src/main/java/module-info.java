module ultimategdbot.command.impl {
	requires discord4j.core.v3.a8b4e4e668;
	requires discord4j.rest.v3.a8b4e4e668;
	requires ultimategdbot.command.api;
	requires ultimategdbot.logic;
	
	provides com.github.alex1304.ultimategdbot.command.api.DiscordCommand with
		com.github.alex1304.ultimategdbot.command.impl.PingCommand,
		com.github.alex1304.ultimategdbot.command.impl.PongCommand;
}