# Self-hosting guide

UltimateGDBot offers native support for self-hosting, meaning that you can run your own instance of UltimateGDBot, useful if you wish to use UltimateGDBot for a Geometry Dash private server (GDPS). This guide will show you how to host the bot yourself.

## Prepare the project

The first step is to get a copy of UltimateGDBot and build the application with the desired parameters. Folllow the steps below thoroughly.

### Prerequisites

Here are the minimum requirements to build and run your own version of UltimateGDBot:

#### Hardware

- 1 GB of RAM
- 2 CPU cores (1 is okay but might be slow)

#### Software

- Java Runtime Environment 8 or above
- Apache Maven
- MySQL or MariaDB with root privileges	
- Git

I let you search on Google for installation tutorials of these pieces of software for your OS.

### Clone the project from GitHub

Run the following in a terminal

```sh
git clone https://github.com/Alex1304/ultimategdbot.git
cd ultimategdbot
```

### Prepare the database

In the newly created directory you have a file called `database.sql`. Run this SQL script in your MySQL/MariaDB database in order to create the necessary tables. 

After that, you need to fill out the global settings for the bot. For now there is only one, `channel_debug_logs`, which allows you to choose a Discord channel where the bot can send debug messages. Create a new entry in the `global_settings` table in the database, assign to the row the ID 1, and put the ID of the desired channel in the `channel_debug_logs` column. This would correspond to the following SQL command:

```sql
INSERT INTO global_settings VALUES (1, XXXXX);
```

with XXXXX being the ID of the desired debug log channel.

This step is very important. If you don't do this, the bot won't work.

### Create a bot account

You need to create a bot account on Discord to run the bot. This can be done by going to [https://discordapp.com/developers/applications/](https://discordapp.com/developers/applications/), creating a new app and creating the bot account in the Bot tab.

### Edit the contents of your `about` command

The official UltimateGDBot has a `about` command that shows info on the bot itself. Info written here might not be accurate for your version of the bot, so you are given the ability to edit the content of the command.

To do so, open the file `src/main/resources/about.txt`. This is a template where you can use the following variables:

- `{{ bot_name }}` - The name of the bot. The value of this variable is assigned when building the application, which is explained in the next section. If you want to mention the bot's name somewhere in your message, use this variable.
- `{{ project_version }}` - The project's version number
- `{{ bot_release_channel }}` - The release channel (for example "stable" or "beta"). As for bot_name, the value is also assigned during build.
- `{{ bot_owner }}` - The bot owner's Discord tag. Depends on the owner ID given during build.
- `{{ server_count }}` - Displays the amount of servers the bot is operating in, at the moment when the `about` command is run.
- `{{ user_count }}` - Displays the amount of users with whom the bot has a server in common
- `{{ bot_auth_link }}` - The bot's authorization link to add the bot to your server. The link is built using the client ID provided as a parameter during build.
- `{{ official_guild_invite }}` - Displays an invite link to the server that you declared as official guild during build. It will try to fetch existing invites made by the user declared as bot owner, if not found the value of this variable will be *[no invite links available]*, and a warning will be sent in the debug logs channel. Remember that the bot needs Manage Server permission in the official guild to fetch invites.

After you're done editing the template, just save the file and go to next section.


## Build the bot application

This is the most delicate step. Indeed, all required information for the bot to run (database credentials, bot token, etc) must be given during the build phase of the app. The command to build the app without parameter is `mvn package`. To give parameters to the build command, the syntax is `-Dparam="value"` and they should be placed before the `package` keyword. Example:

```sh
mvn -Dfirstparam="somevalue" -Dsecondparam="someothervalue" -Dotherparam="anothervalue" package
```

Here are the parameters you effectively need to give to the command:

- `ultimategdbot.release.channel` - The build environment of the bot (whether it's a beta version or a stable version). This affects the behavior of the `about` command, in the first line where the version number and build type is displayed. Note that to change the version number, you need to do it by editing the `pom.xml` file and change the value inside the first occurence of the `<version>` tag.
- `ultimategdbot.prefix.full` - You define the bot's long prefix here. The official bot uses `ugdb!` as long prefix, you are free to change it here.
- `ultimategdbot.prefix.canonical` - You define the bot's short prefix here. The official bot uses `u!` as short prefix, you are free to change it here as well.
- `hibernate.hikari.dataSource.url` - The URL of your MySQL database. The URL is in the following format: `jdbc:mysql://%HOSTNAME%:%PORT%/%DATABASE_NAME%`, where %HOSTNAME% is the hostname or IP address of the database, %PORT% is the connection port of the database (you can omit it if you're using the default one, remember to remove the colon after the hostname if you do so), and %DATABASE_NAME% is the name of the database created by the script you ran previously.
- `hibernate.hikari.dataSource.user` - The username to login to database
- `hibernate.hikari.dataSource.password` - The password to login to database
- `ultimategdbot.client.id` - The Discord client ID of the bot. You can find it in the Discord's developer website where you created the bot account.
- `ultimategdbot.client.token` - The Discord authentication token of the bot. As well as for the client ID, you can find it in the application page of the Discord developer website. You need to go to the Bot tab and reveal the token. Copy and paste it as the value of this parameter
- `ultimategdbot.hierarchy.owner_id` - The ID of the bot owner's Discord account. The user specified here will have access to administration commands of the bot. To get the ID of a Discord account, enable Developer mode in your Discord settings, right click any user and click Copy ID.
- `ultimategdbot.hierarchy.official_guild_id` - The ID of the official Discord server for your instance of the bot. To get the ID, same process as before, right click any server and select Copy ID.
- `ultimategdbot.hierarchy.moderator_role_id` - The ID of the Moderator role in the official server. It is important that the role is part of the official guild provided above. To get a role ID, it's a bit more tricky. To do that, make the role mentionnable and send a mention of the role with a backslash before the @ symbol. Example: `\@moderators`. The numbers wrapped into `<@& >` is the role ID. People with this role will have access to moderation commands on the bot.
- `ultimategdbot.gd_client.id` - The ID of the bot's Geometry Dash account. This is the Geometry Dash account that users need to send a message to in order to link their accounts. Create a new account on Geometry Dash and put the account ID here. If it's from the official Geometry Dash servers, you can use the `profile` command of the official UltimateGDBot to get the account ID. If it's from a GDPS, contact the GDPS owner to get the account ID.
- `ultimategdbot.gd_client.password` - The password of the GD account given previously
- `ultimategdbot.misc.emoji_guild_id.1` and `ultimategdbot.misc.emoji_guild_id.2` - The ID of the Discord server where custom emojis are stored. UltimateGDBot uses custom emojis for Coins, Downloads, Likes, Demon icons, Difficulty icons, etc. Download the custom emojis pack [here](https://drive.google.com/open?id=1nhDUgMjdjui_AxtGqZy65t7HLILyscQD), unzip the file and upload the emojis to Discord. Since there are more than 50 emojis in total, you need to dispatch them in 2 servers, that's why you have 2 parameters to provide. Remember that the bot must be present in those two servers, else the bot won't be able to find the emojis.

All of the parameters above are *required*. There are some parameters that are optional, here's the list of them:

- `ultimategdbot.gd_client.url` - This parameter allows you to specify the URL of a GDPS. The URL must start with `http://` and **must not have a slash at the end**. The default value is `http://www.boomlings.com/database`, the official Geometry Dash server's URL.
- `ultimategdbot.name` - The name you want to give to your instance of the bot. If you're using the bot for a GDPS, you can name it to something like "UltimateGDPSBot" or whatever your GDPS name is. The default value is "UltimateGDBot".

In the end, the build command should look like this (values given here are just examples of course):

```sh
mvn -Dultimategdbot.release.channel="stable" -Dultimategdbot.prefix.full="ugdb!" -Dultimategdbot.prefix.canonical="u!" -Dhibernate.hikari.dataSource.url="jdbc:mysql://localhost/ultimategdbot" -Dhibernate.hikari.dataSource.user="CoolUsername" -Dhibernate.hikari.dataSource.password="MyP@ssword" -Dultimategdbot.client.id="123456789" -Dultimategdbot.client.token="TheTokenHerejxbkhfnkhsbfhsbckhsllhjjjgcSKFQCQCqcGeGSEcEqyVBFj" -Dultimategdbot.hierarchy.owner_id="987456321" -Dultimategdbot.hierarchy.official_guild_id="85274196" -Dultimategdbot.hierarchy.moderator_role_id="786512570000" -Dultimategdbot.gd_client.id="8888888" -Dultimategdbot.gd_client.password="An0th€rP@ssw0rd" -Dultimategdbot.misc.emoji_guild_id.1="9999999999" -Dultimategdbot.misc.emoji_guild_id.2="66666666666" -Dultimategdbot.name="MySuperGDBot" package
```

If it says BUILD SUCCESS, a JAR file has been created in a directory called `target/`, this is the JAR file you will need to run to start the bot. Do not doubleclick the JAR file as it won't do anything, the proper way to run it is via command line `java -jar ultimategdbot.jar`. If you ever need to change one of the parameters, you need to re-run the build command with the correct parameters and grab the new JAR file.

If it says BUILD FAILURE and can't figure out how to fix it, an alternative method exists to give parameters to the build process. **However, if you're using this method, do NOT publish the code of your version of UltimateGDBot, as it involves putting sensitive data into configuration files tracked by Git.** This file is named `pom.xml` and is located in the root folder of the project. Open this file, and scroll to where the `<properties>` tag is located (lines 37 to 64). The parameters listed above are all between lines 44 and 63. For each line, you need to open the tag if closed (if a tag is formatted as `<propertyname />` you need to change is as `<propertyname></propertyname>`), and write the value of the property between the tags (without quotes). In our example above it would look like this:

```xml
<properties>

	<!-- Don't touch properties from lines 38 to 43 -->

	<hibernate.hikari.dataSource.url>jdbc:mysql://localhost/ultimategdbot</hibernate.hikari.dataSource.url>
	<hibernate.hikari.dataSource.user>CoolUsername</hibernate.hikari.dataSource.user>
	<hibernate.hikari.dataSource.password>MyP@ssword</hibernate.hikari.dataSource.password>

	<!-- The properties below must be given as arguments on build (unless it's
			prefilled with a value) -->
	<ultimategdbot.name>MySuperGDBot</ultimategdbot.name>
	<ultimategdbot.client.id>123456789</ultimategdbot.client.id>
	<ultimategdbot.client.token>TheTokenHerejxbkhfnkhsbfhsbckhsllhjjjgcSKFQCQCqcGeGSEcEqyVBFj</ultimategdbot.client.token>
	<ultimategdbot.hierarchy.owner_id>987456321</ultimategdbot.hierarchy.owner_id>
	<ultimategdbot.hierarchy.official_guild_id>85274196</ultimategdbot.hierarchy.official_guild_id>
	<ultimategdbot.hierarchy.moderator_role_id>786512570000</ultimategdbot.hierarchy.moderator_role_id>
	<ultimategdbot.gd_client.url>default</ultimategdbot.gd_client.url>
	<ultimategdbot.gd_client.id>8888888</ultimategdbot.gd_client.id>
	<ultimategdbot.gd_client.password>An0th€rP@ssw0rd</ultimategdbot.gd_client.password>
	<ultimategdbot.prefix.full>ugdb!</ultimategdbot.prefix.full>
	<ultimategdbot.prefix.canonical>u!</ultimategdbot.prefix.canonical>
	<ultimategdbot.release.channel>stable</ultimategdbot.release.channel>
	<ultimategdbot.misc.emoji_guild_id.1>9999999999</ultimategdbot.misc.emoji_guild_id.1>
	<ultimategdbot.misc.emoji_guild_id.2>66666666666</ultimategdbot.misc.emoji_guild_id.2>
</properties>
```

Save the file and then just run `mvn package` without any parameter.

If it still says BUILD FAILURE, try to solve the problem by yourself by looking at the errors displayed in console, and if you are stuck don't hesitate to reach support by following instructions in the Home page of this manual.

## Run the bot

Go in the `target/` directory of the project, you should find a file called `ultimategdbot.jar`. Do not double-click this file, this is not the correct way to run the bot. Instead, open a terminal in this directory and do the command

```
java -jar ultimategdbot.jar
```

If you want to let it run on a Linux dedicated server, use

```
nohup java -jar ultimategdbot.jar >> out.log 2>&1 &
```

Now you can safely log out of your SSH session and you can check the bot's output log at anytime by opening the `out.log` file, located in the same directory as the jar.

## Common errors

When running the JAR file, here are the common errors you may experience:

- `Communications link failure. Last packet sent to the server was 0 ms ago` - The bot was unable to reach the database server. Double check if the URL in your `hibernate.hikari.dataSource.url` parameter has correct hostname, port and database name, and is properly formatted. Check if there isn't any firewall or proxy that may block the connection to your database server.
- `Access denied for [user]@[hostname]` - Your database login/password are incorrect
- `java.lang.NoClassDefFoundError: javafx/collections/ObservableList` - This is an issue with your Java installation. This mostly occurs on Linux with OpenJDK installations. Basically, the bot is using the ObservableList class from the JavaFX Collections API, so you need JavaFX available as a system library. To fix this, it is recommended to uninstall Java and download the JDK from [Oracle's site](http://www.oracle.com/technetwork/java/javase/downloads/index.html) instead (the JRE should work as well). Download the appropriate package for your system architecture, extract the package and add the `bin/` directory from the extracted folder in your PATH environment variable. Then try re-running the JAR and it should work.
- `java.lang.NullPointerException` with a stack trace mentionning `UltimteGDBot.emoji(...)` or `Emojis.get(...)` - There is a problem with the emoji servers. Make sure that you have provided the correct IDs for `ultimategdbot.misc.emoji_guild_id.1` and `ultimategdbot.misc.emoji_guild_id.2`, that you have uploaded all emojis in the two servers, and that your bot is present in those servers (if you don't add the bot in emoji servers, it cannot see them)

The errors listed above are the most common ones. If you have an error not listed here and don't know how to solve it, refer to the Help and Support section in the Home page of this manual to get help.

## Administration commands

As the bot owner or bot moderator, you have access to extra commands that are not listed in the regular commands list.

### `modules`

Owner only. This is where you start and stop modules of the bot. The bot implementation is modular, each module encapsulates a particular functionality. Currently the modules are:

- `commands` - This module registers all commands and exposes them to everyone. If this module is disabled, nobody will be able to use any command, except for the bot owner (so the module can be started again later).
- `reply` - This module handles interactive menus and navigation menus. If this module is disabled, you can no longer navigate through level search results or leaderboards, and commands containing subcommands won't work anymore unless the subcommands are given as arguments. Generally it is deprecated to disable this module, as it can negatively affect users experience. If you have issues with some commands, it's better to disable the whole `commands` module instead.
- `gd_events` - This module encapsulates the scanners for GD events. A scanner is a task that refreshes the Awarded page, the Daily level and the Weekly demon at a periodic rate in order to see if there are changes. This way the bot can detect that a level got rated/Daily/Weekly and automatically notify the configured channels in real time. If this module is disabled, the bot will stop scanning for changes in GD and it's up to the owner to trigger awarded/daily/weekly events using `pushevent`.
- `guild_events` - Just a small module that sends messages in the debug log channel everytime people add or remove the bot from their Discord server. It's useful to keep track of the servers the bot joins or leaves.

To start a module, use the `start` subcommand followed by the name of the module. Example:

```
[prefix]modules start gd_events
```

To stop a module, use the `stop` subcommand followed by the name of the module. Example:

```
[prefix]modules stop commands
```

When a module starts or stops, a message is sent in the debug log channel. When the bot starts, all modules are enabled by default. So if you disable a module and then restart the bot (for example using the `restart` command), this module will be re-enabled.

### `pushevent`

Owner only. Allows you to manually trigger a Geometry Dash event.

```
[prefix]pushevent 1 12345678
```

will trigger a new rated level notification for level with ID 12345678. Note that only level IDs are supported here, you cannot give a level name. See the instructions on the pushevent command message to know the other events you can push.

### `restart`

Owner and moderators. Allows you to restart the whole bot. It terminates the JVM and starts the JAR file as a separate process.

## `shutdown`

Owner only. Shuts down the bot completely. The JVM is terminated and the bot will go offline.

## `system`

Owners and moderators. Allows you to monitor the system resources that the bot is consuming. It gives info on both memory usage and CPU load.

For further help with self-hosting, see the Help & Support section in the homepage of this manual.