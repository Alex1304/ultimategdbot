# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UltimateGDBot is a Discord bot for Geometry Dash players, running in 21,000+ servers. It is a Java 17, Maven multi-module project using reactive programming (Project Reactor) throughout.

## Build Commands

```bash
# Full build and package
mvn clean package

# Build with a specific JDK (for jlink packaging)
mvn clean package -Djlink.jdk="/path/to/jdk17"

# Run unit tests
mvn test -pl app

# Release (sets version, builds multi-platform, deploys, bumps to next snapshot)
./release.sh <release-version> <next-snapshot-version>
```

## Testing

Tests cover the **service layer** only — Discord4J entities are not mocked. The stack is JUnit 5 + `StepVerifier` (Reactor Test) + a `GDRouter` lambda test double injected via `GDClient.create().withRouter(router)`. Services are instantiated directly (bypassing RDI), passing `null` for any dependency the method under test doesn't use. See `.claude/rules/testing.md` for full details.

## Architecture

### Module Structure

```
ultimategdbot (parent pom)
├── app/        — All business logic, commands, services, database entities
├── launcher/   — CLI wrapper that spawns the bot subprocess
└── delivery/   — jlink packaging and distribution zips
```

### Technology Stack

- **Framework:** [Botrino](https://botrino.alex1304.com) — a Discord4J wrapper providing DI (RDI), slash commands, and lifecycle management
- **Discord:** Discord4J via Botrino
- **GD API:** JDash — `jdash-events` for polling, `jdash-graphics` for rendering
- **Database:** MongoDB with Immutables Criteria ORM (reactive, type-safe, generated repositories)
- **Reactive:** Project Reactor (`Mono`/`Flux`) used pervasively — all I/O is non-blocking
- **Code Generation:** Immutables (`@Value.Immutable`) for value objects and DB entities; run `mvn generate-sources` to regenerate

### Linked projects

Jdash and Botrino, which UltimateGDBot depends on, are both projects that are self-maintained by alex1304 along with this one. Their respective git repository is cloned in the parent directory of this project. When appropriate, you may choose to go and edit files in these projects, by preferable spawning a separate agent in that directory that will load rule files in a fresh context.

A clone of Discord4J may also be present, but it is only there for alex1304 to contribute to D4J via PRs, it is irrelevant for us and should not be explored unless explicitly told otherwise.

### App Module Package Layout (`app/src/main/java/ultimategdbot/`)

| Package | Responsibility |
|---|---|
| `command/` | Discord slash command handlers (14 commands) |
| `service/` | Business logic: GD data, blacklisting, logging, rate limiting, i18n, pagination |
| `database/` | MongoDB entity interfaces + generated Immutable implementations |
| `config/` | `UltimateGDBotConfig` — reads `config.json`; also `MongoDBConfig` |
| `event/` | GD event polling loop, Discord channel subscriptions, crosspost queue |
| `framework/` | Botrino extension (`UltimateGDBotExtension`), login handler, error handler |
| `util/` | Formatting helpers, embed templates, Discord interaction utilities |

### Key Architectural Patterns

- **Dependency Injection:** Services are registered in `UltimateGDBotExtension` using Botrino's RDI framework.
- **Immutables:** All DB entities and config classes are `@Value.Immutable` interfaces. Generated sources appear in `target/generated-sources/`. Do not edit generated `Immutable*` classes directly.
- **Event Pipeline:** `GDEventService` polls the GD API on a configurable interval; events fan out through `GDEventSubscriber` to subscribed Discord channels, with crosspost support via `CrosspostQueue`.
- **i18n:** 11 locales via `ResourceBundle` properties files in `app/src/main/resources/`. Crowdin manages translations (`crowdin.yml`). String keys are accessed through `Strings.java`.

### Runtime Flow

1. `Launcher.java` — CLI entry point, launches the bot as a subprocess with module classpath
2. `Main.java` — sets default `Locale.ENGLISH`, configures Reactor error hooks, calls `Botrino.run()`
3. `UltimateGDBotExtension` — registers all services and commands with the Botrino container
4. Bot connects to Discord; `GDEventService` starts the event polling loop

### Configuration

Runtime configuration lives in `app/src/main/external-resources/config.json` (template with `${placeholder}` values for secrets). Key sections:
- `bot.token` — Discord bot token
- `mongodb` — connection string and database name
- `ultimategdbot.gd.client` — GD API credentials, cache TTL, rate limits
- `ultimategdbot.gd.events` — polling interval, channel IDs for rated/demon events, crosspost flag
