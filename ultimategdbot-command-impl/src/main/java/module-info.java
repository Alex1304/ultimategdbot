module com.github.alex1304.ultimategdbot.core {
	requires discord4j.core.ae68630;
	requires com.github.alex1304.ultimategdbot.command.api;
	
	provides com.github.alex1304.ultimategdbot.command.api.DiscordCommand with com.github.alex1304.ultimategdbot.command.impl.PingCommand;
}