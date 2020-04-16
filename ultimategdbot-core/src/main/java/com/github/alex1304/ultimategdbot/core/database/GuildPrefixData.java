package com.github.alex1304.ultimategdbot.core.database;

import java.util.Optional;

import org.immutables.value.Value;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigData;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigurator;
import com.github.alex1304.ultimategdbot.api.guildconfig.StringConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.Validator;

import reactor.core.publisher.Mono;

@Value.Immutable
public interface GuildPrefixData extends GuildConfigData<GuildPrefixData> {
	
	Optional<String> prefix();

	@Override
	default GuildConfigurator<GuildPrefixData> configurator(Bot bot) {
		return GuildConfigurator.builder(this)
				.addEntry(StringConfigEntry.<GuildPrefixData>builder("core.prefix")
						.setDescription("Configure the command prefix to use for this server.")
						.setValueGetter(data -> Mono.justOrEmpty(data.prefix()))
						.setValueSetter((data, value) -> ImmutableGuildPrefixData.builder()
								.from(data)
								.prefix(value)
								.build())
						.setValidator(Validator.denyingIf(String::isBlank, "cannot be blank"))
						.setValueObserver(value -> bot.commandKernel().setPrefixForGuild(guildId().asLong(), value)))
				.build();
	}
}
