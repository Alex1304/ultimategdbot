package com.github.alex1304.ultimategdbot.core.database;

import java.util.Optional;

import org.immutables.value.Value;

import com.github.alex1304.ultimategdbot.api.Bot;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildChannelConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigData;
import com.github.alex1304.ultimategdbot.api.guildconfig.GuildConfigurator;
import com.github.alex1304.ultimategdbot.api.guildconfig.LongConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.StringConfigEntry;
import com.github.alex1304.ultimategdbot.api.guildconfig.Validator;

import reactor.core.publisher.Mono;

@Value.Immutable
public interface GuildPrefixData extends GuildConfigData<GuildPrefixData> {
	
	Optional<String> prefix();

	@Override
	default GuildConfigurator<GuildPrefixData> configurator(Bot bot) {
		return GuildConfigurator.builder("Server Prefix", this, GuildPrefixDao.class)
				.setDescription("The command prefix that the bot will respond to in this server.")
				.addEntry(StringConfigEntry.<GuildPrefixData>builder("prefix")
						.setValueGetter(data -> Mono.justOrEmpty(data.prefix()))
						.setValueSetter((data, value) -> ImmutableGuildPrefixData.builder()
								.from(data)
								.prefix(Optional.ofNullable(value))
								.build())
						.setValidator(Validator.denyingIf(String::isBlank, "cannot be blank")))
				.addEntry(LongConfigEntry.<GuildPrefixData>builder("random_count")
						.setDisplayName("random count")
						.setPrompt("new random count for testing purposes")
						.setValueSetter((data, value) -> data)
						.setValidator(Validator.allowingIf(l -> l >= 20, "must be greater than or equal to 20")))
				.addEntry(GuildChannelConfigEntry.<GuildPrefixData>builder("secret_channel")
						.setDisplayName("secret channel")
						.setPrompt("channel where to keep things secret")
						.setValueSetter((data, value) -> data))
				.onSave(data -> bot.commandKernel().setPrefixForGuild(guildId().asLong(), data.prefix().orElse(null)))
				.build();
	}
}
