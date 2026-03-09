---
description: Rules for slash command handlers in the command/ package
globs: ["**/command/**/*.java"]
---

# Command Rules

## Class structure

A simple command is a `@RdiService final class` implementing one or more listener interfaces:

```java
@RdiService
@ChatInputCommand(name = "foo", description = "Does foo.")
public final class FooCommand implements ChatInputInteractionListener {

    private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);

    @RdiFactory
    public FooCommand(/* deps */) { ... }
```

## Subcommands

When a command has subcommands, the outer class has **no** `@RdiService` — each subcommand is a `@RdiService public static final class` inside it:

```java
@ChatInputCommand(name = "foo", description = "...", subcommands = {
        @Subcommand(name = "bar", description = "...", listener = FooCommand.Bar.class)
})
public final class FooCommand {

    @RdiService
    public static final class Bar implements ChatInputInteractionListener {
        @RdiFactory
        public Bar(...) { ... }
```

## The run() method

- Return type is always `Publisher<?>`.
- For `ChatInputInteractionListener`, always start with `ctx.event().deferReply()`:
  ```java
  @Override
  public Publisher<?> run(ChatInputInteractionContext ctx) {
      return ctx.event().deferReply().then(grammar.resolve(ctx.event()))
              .flatMap(options -> ...);
  }
  ```
- For `UserInteractionListener`, use `ctx.event().deferReply().withEphemeral(true)`.
- Send the response with `ctx.event().createFollowup(...)` (not `reply`).

## Options

Options are always a `private record` inside the command:

```java
private record Options(
        @ChatInputCommandGrammar.Option(
                type = ApplicationCommandOption.Type.STRING,
                name = "gd-username",
                description = "The GD username.",
                required = true
        )
        String gdUsername
) {}
```

Override `options()` to return `grammar.toOptions()`. Required options use `required = true`.

## Error handling

- Throw `InteractionFailedException` for user-facing errors (invalid input, not found, etc.).
- Throw `RetryableInteractionException` for transient errors the user can retry.
- Never throw generic `RuntimeException` for user errors.

## Cooldown and i18n

- Inject `GDCommandCooldown` and override `cooldown()` to return `commandCooldown.get()` **only for commands that call the GD API via jdash** — the GD API has strict rate limits and this prevents users from triggering them too often. Commands that don't hit the GD API do not need a cooldown.
- All user-facing strings go through `ctx.translate(Strings.GD, "key")` or `ctx.translate(Strings.GENERAL, "key")` — no hardcoded strings.

## Component interactions (buttons, select menus)

- Generate IDs with `UUID.randomUUID().toString()`.
- Await with `ctx.awaitButtonClick(id)` or `ctx.awaitSelectMenuItems(id)`.
- Apply `.timeout(ctx.getAwaitComponentTimeout())` to the await call.
- Clean up the followup message on both success and timeout: `.then(ctx.event().deleteFollowup(messageId)).onErrorResume(deleteFollowupAndPropagate(ctx, messageId))`.
