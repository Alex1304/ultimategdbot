# UltimateGDBot

![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/ultimategdbot/ultimategdbot?include_prereleases&sort=semver)
![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/ultimategdbot-api)
![License](https://img.shields.io/github/license/ultimategdbot/ultimategdbot)
[![javadoc](https://javadoc.io/badge2/com.github.alex1304/ultimategdbot-api/javadoc.svg)](https://javadoc.io/doc/com.github.alex1304/ultimategdbot-api) 
[![Official Server](https://img.shields.io/discord/357655103768887297?color=%237289DA&label=Official%20Server&logo=discord)](https://discord.gg/VpVdKvg)
[![Crowdin](https://badges.crowdin.net/ultimategdbot/localized.svg)](https://crowdin.com/project/ultimategdbot)


High-level API to conveniently build Discord bots in Java, on top of [Discord4J](https://discord4j.com).

## Notice

**This project is at an early stage of its development and is not production-ready. Some parts of the API may have bugs and/or do not have solid structures and are subject to breaking changes until a stable release is made for the 6.x branch**.

On another note, the `5.x` branch is not really an API but an artifact of the original UltimateGDBot project which was a Discord bot for Geometry Dash. This bot still exists, but the Geometry Dash features have been moved to a plugin implementing this new API, that you can find [here](https://github.com/ultimategdbot/ultimategdbot-gd-plugin).

Everything in this `README` only concerns the 6.x (master) branch, which is still in development. 

## What is UltimateGDBot?

UltimateGDBot allows to make plugin-oriented bots. Everything related to how your bot communicates with Discord is abstracted behind a single `Bot` interface.
A plugin is what will allow you to add features to this bot. The goal is that you write only the features you want to see in your bot, the library does the rest for you.

## Getting started

This library requires the JDK 11. You can download the OpenJDK [here](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot).

### Maven

```xml
<dependencies>
	<dependency>
		<groupId>com.github.alex1304</groupId>
		<artifactId>ultimategdbot-api</artifactId>
		<version>6.0.0-alpha1</version>
	</dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
	mavenCentral()
}

dependencies {
	implementation 'com.github.alex1304:ultimategdbot-api:6.0.0-alpha1'
}
```

## How it works

### Example of plugin definition

```java
public class ExamplePluginBootstrap implements PluginBootstrap {

	@Override
	public Mono<Plugin> setup(Bot bot) {
		// Define some commands
		var commandProvider = new AnnotatedCommandProvider();
		commandProvider.add(new HelloCommand());

		// Build the plugin
		var plugin = Plugin.builder("Example")
				.setCommandProvider(commandProvider)
				.build();

		// Return it
		return Mono.just(plugin);
	}
}
```

Since UltimateGDBot uses the Java module system to load the plugins, you need to declare a `module-info.java` with the following minimal contents:

```java
open module example.plugin {

	requires ultimategdbot.api;

	provides PluginBootstrap with ExamplePluginBootstrap;
}
```

### Example command

Writing commands with the UltimateGDBot command API is very easy:

```java
@CommandDescriptor(
	aliases = "hello",
	shortDescription = "Responds to the user with \"hello\"."
)
class HelloCommand {

	@CommandAction
	public Mono<Void> run(Context ctx) {
		return ctx.reply("hello").then();
	}
}
```
Use annotations to provide all the meta-information you would give to any bot command (aliases, help message, permission level, etc).
You can access information such as who ran the command, what are the arguments, etc via the `Context` object.
The API makes uses of the Reactor types (`Flux` and `Mono`) to give async capabilities to the bot, you can read more about Reactor [here](https://projectreactor.io/docs/core/release/reference/).

## Useful Links

* [UltimateGDBot Javadoc](http://javadoc.io/doc/com.github.alex1304/ultimategdbot-api/)
* [Discord4J Site](https://discord4j.com)
* [Discord4J Wiki](https://github.com/Discord4J/Discord4J/wiki)
* [Discord4J Javadoc](http://javadoc.io/doc/com.discord4j/discord4j-core/)
* [Reactor 3 Reference Guide](http://projectreactor.io/docs/core/release/reference/)

## Contributing

You may use snapshot and alpha/beta versions if you want to report bugs and give feedback on this project. Issues and pull requests on the master branch are welcome, just follow the instructions on the issue creation menu.

## License

This project is released under the MIT license.

## Contact

E-mail: ultimategdbot@gmail.com

Discord: `Alex1304#9704` (Server [https://discord.gg/VpVdKvg](https://discord.gg/VpVdKvg))

Twitter: [@gd_alex1304](https://twitter.com/gd_alex1304)