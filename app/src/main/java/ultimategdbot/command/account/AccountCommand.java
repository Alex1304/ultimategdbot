package ultimategdbot.command.account;

import botrino.interaction.annotation.ChatInputCommand;
import botrino.interaction.annotation.Subcommand;

@ChatInputCommand(
        name = "account",
        description = "Manage your connection with your Geometry Dash account.",
        subcommands = {
                @Subcommand(
                        name = "status",
                        description = "Shows your account linking status.",
                        listener = StatusSubcommand.class
                ),
                @Subcommand(
                        name = "link",
                        description = "Link a Geometry Dash account to your Discord account.",
                        listener = LinkSubcommand.class
                ),
                @Subcommand(
                        name = "unlink",
                        description = "Unlink your Geometry Dash account from your Discord account.",
                        listener = UnlinkSubcommand.class
                ),
                @Subcommand(
                        name = "settings",
                        description = "View or modify settings that are applied to your account.",
                        listener = SettingsSubcommand.class
                )
        }
)
public final class AccountCommand {}
