# Commands documentation

This page will list all commands available in UltimateGDBot.

Some of the following commands have subcommands. Subcommands are commands that are triggered either via an interactive menu (when you are prompted to type a word in chat), or by passing them directly in arguments inline with the base command (doesn't work for all commands). For each command listed below, the list of subcommands is documented when applicable.

Remember to replace `[prefix]` by the actual prefix in the given examples

## `about`

Command that shows general info on UltimateGDBot, as well as the bot authorization link, the invite to the official Discord server of UltimateGDBot and other credits.

## `account`

This command allows you to associate your Geometry Dash account with your Discord account. Linking your account allows UltimateGDBot to etablish a mapping between Geometry Dash users and Discord users, which can unlock a lot of possibilities. For example you can use some commands by tagging directly a Discord user instead of typing his GD username, build a server-wide Geometry Dash leaderboard (see `leaderboard` command), and more.

To link your account, use the subcommand `link` which takes in argument your Geometry Dash username or player ID. Example (each new line is a separate message):

```
[prefix]account

link ExamplePlayer01
```

or all inline

```
[prefix]account link ExamplePlayer01
```

Then you need to follow instructions given by the command to complete the linking process. When you have followed all instructions, use the subcommand `done`, or `[prefix]account done` if it doesn't work. To unlink your account, use the subcommand `unlink`. Note that you can link several Discord accounts to the same GD account, but you can't link several GD accounts to the same Discord account. This is designed so if you lose access to your Discord account, you can still use a new Discord account to link.

## `checkmod`

This command checks for the presence of a Moderator badge on the specified GD user's profile. The bot replies with "Failed" if the user isn't moderator, otherwise it replies "Success". This is designed similarly to the "REQ" button in-game, however keep in mind that the doesn't use this button behind the scenes. It just checks for the presence of the Moderator badge on the user's profile, as said previously.

Why is this command useful? It is true that the `profile` command already shows the Moderator status, but `checkmod` has the particularity of being able to detect changes in the moderator status of the user. If you run `checkmod` once, and says "Failed", then re-run the command a few moments later and says "Success" on the same user, then a Geometry Dash event "User promoted!" will be fired, and all servers that have configured a channel to receive GD moderators notifications will receive a message. This command is actually the only way to trigger GD moderator events, so if you know that someone got promoted or demoted, go ahead and use `checkmod` on that user to let everyone else know!

```
[prefix]checkmod Viprin
```

Or if you linked your Geometry Dash account to Discord, the following will check your own mod status:

```
[prefix]checkmod
```

See `acccount` command for details.

## `daily`

Shows info on the current Daily level in Geometry Dash. The countdown to next Daily level is displayed in the message

## `gdevents`

Please refer to [this section](http://ultimategdbot.readthedocs.io/en/latest/Configure-Geometry-Dash-event-notifications/) to get explanation on this command.

## `help`

Gives a quick list of commands, contains info on the bot's prefixes, and has a link to the user manual you're reading right now.

## `leaderboard`

Shows a Geometry Dash leaderboard against server members. You can select which type of leaderboard you want to show (stars, diamonds, demons, etc). Note that only members that have linked their Geometry Dash account using the `account` command will be shown in the leaderboard, otherwise the bot can't know if a GD user is a member of your server.

```
[prefix]leaderboard

stars
```

or

```
[prefix]leaderboard stars
```

will display a leaderboard that sorts players by their amount of stars.

It has the same subcommands as `level` and `modlist` commands to navigate through the leaderboard (aka `next`, `prev` and `page` followed by a page number). But the `leaderboard` command also has a subcommand called `finduser`, which allows you to find a specific user in the leaderboard. Assuming you've already shown the leaderboard,

```
finduser ExamplePlayer01
```

will try to find a user called `ExamplePlayer01` in the leaderboard. If no user is found, nothing will change. If the user is found, you will be taken to the spot where the user is placed.

## `level`

This command allows you to search for online levels in Geometry Dash. You can search either by name or by ID. If your search query produces only 1 result, the level will directly be shown to you with full info on the level. If your search query produces more than 1 result, you will be first taken to a navigation menu from which you can select the level you want to display info about. The navigation menu has several subcommands to navigate through results, such as `next`, `prev` or `page` followed by a page number. To select a level, use the `select` subcommand which takes the item number in argument.

```
[prefix]level Bloodbath

select 1
```

The example above searches for level "Bloodbath". Once all results are showing, `select 1` opens the first result.

## `modlist`

Shows the last known list of Geometry Dash moderators. This list is updated as GD mod events are being fired thanks to the `checkmod` command. This command also has a navigation system that allows you to navigate through the list of mods (20 mods per page are displayed).

## `ping`

Simple command that replies with "Pong!". This is just to check if the bot is alive, there is no use for this command.

## `profile`

Shows a user's profile from Geometry Dash. The bot shows the user's stats (stars, diamonds, secret and user coins, demons, creator points) as well as the global rank, moderator status, and social links. You can use this command without specifying any argument to display your own profile, unless you haven't linked your Geometry Dash account with Discord. See `account` command for details.

```
[prefix]profile RobTop
```

## `setup`

Please refer to [this section](http://ultimategdbot.readthedocs.io/en/latest/Configure-Geometry-Dash-event-notifications/) to get explanation on this command.

## `weekly`

Shows info on the currentWeekly demon in Geometry Dash. The countdown to next Weekly demon is displayed in the message.