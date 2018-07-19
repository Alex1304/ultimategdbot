# Commands documentation

This page will list all commands available in UltimateGDBot.

Some of the following commands have subcommands. Subcommands are commands that are triggered either via an interactive menu (when you are prompted to type a word in chat), or by passing them directly in arguments inline with the base command (doesn't work for all commands). For each command listed below, the list of subcommands is documented when applicable.

## `about`

Command that shows general info on UltimateGDBot, as well as the bot authorization link, the invite to the official Discord server of UltimateGDBot and other credits.

## `account`

This command allows you to associate your Geometry Dash account with your Discord account. Linking your account allows UltimateGDBot to etablish a mapping between Geometry Dash users and Discord users, which can unlock a lot of possibilities. For example you can use some commands by tagging directly a Discord user instead of typing his GD username, build a server-wide Geometry Dash leaderboard (see `leaderboard` command), and more.

To link your account, use the subcommand `link` which takes in argument your Geometry Dash username or player ID. Example (each new line is a separate message):

```
u!account

link ExamplePlayer01
```

or all inline

```
u!account link ExamplePlayer01
```

Then you need to follow instructions given by the command to complete the linking process. When you have followed all instructions, use the subcommand `done`, or `u?account done` if it doesn't work.