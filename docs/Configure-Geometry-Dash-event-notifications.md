# Configure Geometry Dash event notifications

One of the core features of UltimateGDBot is the ability to send notifications when specific events happens in Geometry Dash. In other words, UltimateGDBot can send a notification when:

- New levels are getting rated or unrated by RobTopGames (aka new levels appearing in the Awarded page in game)
- RobTopGames changes the Daily level or the Weekly demon
- RobTopGames promotes or demotes Geometry Dash moderators

When one of those events happen, a message will be sent in your server in a channel of your choice. For each type of event you can also configure a role to tag in the notification message. This section will show you how to configure all of that in your server.

## The `setup` command

This is the command that will allow you to configure UltimateGDBot for your server so you can start receiving notifications. Only users with the Administrator permission can use this command.

When you run this command, a menu opens and displays the current settings. If you run this command for the first time, everything will display as `N/A` or some default value, which is normal. Each row has a label and a value, both separated by a colon and a space. Here are the signification of the different labels in the settings table:

- `channel_awarded_levels` - The channel where the bot is supposed to send notifications on new rated and unrated levels. Expected value: the name or the link of a Discord channel, ex. `general` or `#general`
- `channel_timely_levels` - The channel where the bot is supposed to send notifications on new Daily levels and Weekly demons. Expected value: the name or the link of a Discord channel, ex. `general` or `#general`
- `channel_gd_moderators` - The channel where the bot is supposed to send notifications on Geometry Dash moderators promotions/demotions. Expected value: the name or the link of a Discord channel, ex. `general` or `#general`
- `role_awarded_levels` - The role that the bot is supposed to tag when sending notifications on new rated and unrated levels. Expected value: the name or the mention of a Discord role, ex. `subscribers` or `@subscribers`
- `role_timely_levels` - The role that the bot is supposed to tag when sending notifications on new Daily levels and Weekly demons.. Expected value: the name or the mention of a Discord role, ex. `subscribers` or `@subscribers`
- `role_gd_moderators` - The role that the bot is supposed to tag when sending notifications on Geometry Dash moderators promotions/demotions. Expected value: the name or the mention of a Discord role, ex. `subscribers` or `@subscribers`

Let's say I want to make the bot send notification for new awarded levels in a channel called `#botspam` and by tagging the role `@pingme` on each notification. I would run the following sequence of commands (each new line is a separate message):


```
[prefix]setup

set channel_awarded_levels botspam

set role_awarded_levels pingme
```

The `set` subcommand allows you to edit the value of a field. The first argument is the label of the setting you want to edit, and the second argument is the value you want to assign to this field.

From now on, the bot should be sending messages in the specified channel and the specified role will be tagged at each notification. If it doesn't send anything even after the event happened, **please double check if the bot has permissions to send messages with embeds links in the specified channel**. Note that the role setting is optional: if you configure a channel without specifying a role to tag, the notifications feature will still work, the messages will just be sent in the channel without tagging any role.

If you want the bot to stop sending notifications, use the `reset` subcommand on the channel you want the bot to stop sending notifications. For example, 

```
[prefix]setup reset channel_awarded_levels

```

will make the bot stop sending awarded level notifications. To enable it back, use the `set` command again on the desired channel.

## The `gdevents` command

Once you have configured roles to tag, everyone in your server can assign these roles themselves using the `gdevents` command. 
Running this command will prompt you the list of events that have a role configured. Typing one of the given labels will assign the role to you (or remove if you had it already). Example:

```
u!gdevents awarded_levels
```

The command above will give me the `@pingme` role, if we consider the configuration example given earlier. **It is important that UltimateGDBot has the permission to manage roles in your server, or this command would be unusable**. Also make sure that the role doesn't have high permissions, to avoid users be granted permissions in your server when they receive the role. It's up to you to check that, the bot doesn't. It would be very bad if the `@pingme` role could give users Ban permissions!