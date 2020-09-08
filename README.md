# UltimateGDBot

![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/ultimategdbot/ultimategdbot?include_prereleases&sort=semver)
![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/ultimategdbot-api)
![License](https://img.shields.io/github/license/ultimategdbot/ultimategdbot)
[![javadoc](https://javadoc.io/badge2/com.github.alex1304/ultimategdbot-api/javadoc.svg)](https://javadoc.io/doc/com.github.alex1304/ultimategdbot-api) 
[![Official Server](https://img.shields.io/discord/357655103768887297?color=%237289DA&label=Official%20Server&logo=discord)](https://discord.gg/VpVdKvg)
[![Crowdin](https://badges.crowdin.net/ultimategdbot/localized.svg)](https://crowdin.com/project/ultimategdbot)


High-level API to conveniently build Discord bots in Java, on top of [Discord4J](https://discord4j.com).

## Notice

**This library is not yet fully documented.** The quick start instructions given in this README are of course insufficient to master the library and make full-fledged bots, full documentation will be available as soon as possible in a later update. In the meantime, you can take a look at the source code of the [UlitmateGDBot Core Plugin](https://github.com/ultimategdbot/ultimategdbot-core-plugin) which is a common plugin that provides essential commands for a Discord bot (such as `!help`, `!ping`, etc).

## What is UltimateGDBot?

UltimateGDBot allows to make plugin-oriented bots. A plugin is what will allow you to add features to your bot. The goal is that you write only the features you want to see in your bot with the least amount of boilerplate code possible, the library does (almost) all the boring stuff for you.

## Getting started

This library requires the JDK 11 or newer. You can download the OpenJDK [here](https://adoptopenjdk.net/?variant=openjdk11&jvmVariant=hotspot).

### Maven

```xml
<dependencies>
	<dependency>
		<groupId>com.github.alex1304</groupId>
		<artifactId>ultimategdbot-api</artifactId>
		<version>[VERSION]</version>
	</dependency>
</dependencies>
```

Where `[VERSION]` is the latest as per ![Maven Central](https://img.shields.io/maven-central/v/com.github.alex1304/ultimategdbot-api)

### Gradle

```groovy
repositories {
	mavenCentral()
}

dependencies {
	implementation 'com.github.alex1304:ultimategdbot-api:[VERSION]'
}
```

## How it works

Most of the boilerplate code that you will write when working with this library will be at the start, when setting up your plugin. Each plugin can declare one or more *services*, which will do specific actions such as loading commands, processing data from database, and anything you want your Discord bot to do.

Services are initialized using the principle of **dependency injection**, using the [RDI library](https://github.com/Alex1304/rdi). Each service may depend on other services, either made by yourself, provided by the library (e.g `BotService`), or even from external parties. You define the service hierarchy of your plugin through RDI's `ServiceDescriptor`s that you expose via an implementation of `ServiceDeclarator`. If you didn't understand anything don't worry, maybe a concrete example will make everything clear.

> Note: the API makes use of the Reactor types (`Flux` and `Mono`) to give async capabilities to the bot, you can read more about Reactor [here](https://projectreactor.io/docs/core/release/reference/).

### Example of a basic plugin

The first step is to declare a service for your plugin, that will serve as root. In this example, this service will only be in charge of loading commands.

```java
public class MyService {

	private final BotService bot;

	private MyService(BotService bot) {
		this.bot = bot;
	}

	public static Mono<MyService> create(BotService bot) {
		return RootServiceSetupHelper.create(() -> new MyService(bot))
				.addCommandProvider(bot.command(), new CommandProvider("MyCommands"))
				.setup();
	}
}
```

The class `RootServiceSetupHelper` is what will allow you to autoload your commands from the module path.

Before adding a command, you need to declare the service you just created, so that it can be loaded on startup by the bot launcher. To achieve this, create a subclass of `ServiceDeclarator` like so:

```java
public class MyServices implements ServiceDeclarator {

	public static final ServiceReference<MyService> ROOT = ServiceReference.ofType(MyService.class);

	@Override
	public Set<ServiceDescriptor> declareServices(BotConfig botConfig) {
		return Set.of(
			ServiceDescriptor.builder(ROOT)
						.setFactoryMethod(FactoryMethod.staticFactory("create", Mono.class, Injectable.ref(CommonServices.BOT)))
						.build()
		);
	}
}
```

`ROOT` is just a reference that will uniquely identify your service. The `setFactoryMethod` line allows to specify the method that creates your service. In this case, it's a static method called "create" which returns a `Mono` and takes `BotService` as parameter. `CommonServices.BOT` is what allows to inject `BotService` into your service.

Next step is to create the plugin. For that, just create a subclass of `Plugin`:

```java
public class MyPlugin implements Plugin {

	@Override
	public ServiceReference<?> rootService() {
		return MyServices.ROOT;
	}
	
	@Override
	public Mono<PluginMetadata> metadata() {
		return Mono.just(PluginMetadata.builder("MyPlugin").build());
	}
}
```

It simply specifies which service should be used as root, and meta information on the plugin (like the name of the plugin, the version, who created it, etc). Here we only provided the name of the plugin, which is the only information mandatory.

In order to make all of this work, since UltimateGDBot uses the Java module system to load the plugins and the services, you need to declare a `module-info.java` with the following minimal contents:

```java
open module example.plugin {

	requires ultimategdbot.api;

	provides ServiceDeclarator with MyServices;
	provides Plugin with MyPlugin;
}
```

The module must be `open` so that the launcher can initialize your plugin, and must expose your `ServiceDeclarator` and your `Plugin` via `provides` instructions.

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
Your command will automatically be added thanks to the `RootServiceSetupHelper` of your root service, so no need to worry about doing `new HelloCommand()` anywhere in your code!

## How to run the bot?

See the [UlitmateGDBot launcher](https://github.com/ultimategdbot/ultimategdbot-launcher) project to run your plugins created with this library.

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