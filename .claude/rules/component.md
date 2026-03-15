---
description: Rules for Discord Components V2 in the component/ package
globs: ["**/component/**/*.java"]
---

# Components V2 Rules

All new commands should use Discord's Components V2 for message content rather than embeds. Components are composed via
`ComponentsV2Composer.composeMessage` (from Botrino).

## CustomMessageComponent

Each reusable visual component implements `CustomMessageComponent`:

```java
public record FooComponent(
        InteractionContext ctx,
        /* other data */
) implements CustomMessageComponent {

    @Override
    public TopLevelMessageComponent defineComponent() {
        // Build and return a Container (or other top-level component)
        return Container.of(...)
    }

    @Override
    public Flux<? extends MessageCreateFields.File> provideFiles() {
        // Return any files (images, etc.) needed by the component
        // Default implementation returns Flux.empty() — override only if files are needed
        return Flux.fromIterable(...);
    }

```

- Use a `record` for immutable data; no need for a class unless state is mutable.
- `defineComponent()` returns a `TopLevelMessageComponent` — typically a `Container` wrapping `TextDisplay`, `Section`,
  `Separator`, `Thumbnail`, etc.
- `provideFiles()` is optional — only override when the component references file attachments (e.g.
  `attachment://foo.png`).

## Composing a message with ComponentsV2Composer

Use `ComponentsV2Composer.composeMessage(component, actionRowSuppliers...)` to produce the final message spec. Action
rows (buttons, select menus) are passed as additional `Supplier<ActionRow>` arguments:

```java
ComponentsV2Composer.composeMessage(
        new FooComponent(ctx, data),
        () -> ActionRow.of(Button.primary(buttonId, "Click me")),
        () -> Interactions.paginationButtons(ctx, state)
)
```

The result is passed directly to `ctx.event().createFollowup(...)` or equivalent.

## Layout building blocks

Prefer these Discord4J component types for structuring content inside a `Container`:

| Type          | Use for                                                          |
|---------------|------------------------------------------------------------------|
| `TextDisplay` | Text content (supports markdown)                                 |
| `Section`     | Side-by-side content + accessory (e.g. `Thumbnail`)              |
| `Separator`   | Visual divider between sections                                  |
| `Thumbnail`   | Small image accessory inside a `Section`                         |
| `Container`   | Top-level wrapper; pass a `List<ICanBeUsedInContainerComponent>` |
