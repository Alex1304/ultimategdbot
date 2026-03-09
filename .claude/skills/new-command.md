Create a new Discord slash command in this project.

Ask the user for:
1. The command name (kebab-case, e.g. "my-command")
2. The command description (shown in Discord)
3. Whether it has subcommands (yes/no); if yes, ask for each subcommand name and description
4. What options/arguments it takes (name, type, description, required?)
5. Which services it needs to inject

Then generate the command class following these rules exactly:

**Simple command (no subcommands):**
- Place in `app/src/main/java/ultimategdbot/command/`
- Class name: PascalCase of the command name + `Command`, e.g. `MyCommand`
- Annotate with `@RdiService`, `@ChatInputCommand(name = "...", description = "...")`
- `public final class` implementing `ChatInputInteractionListener`
- Always inject `GDCommandCooldown commandCooldown` as a dependency
- Always declare `private final ChatInputCommandGrammar<Options> grammar = ChatInputCommandGrammar.of(Options.class);`
- `@RdiFactory` constructor assigning all deps to `private final` fields
- `run()` returns `Publisher<?>`, starts with `ctx.event().deferReply().then(grammar.resolve(ctx.event())).flatMap(options -> ...)`
- Sends response via `ctx.event().createFollowup(...)`
- `options()` returns `grammar.toOptions()`
- `cooldown()` returns `commandCooldown.get()`
- Options defined as a `private record Options(...)` with `@ChatInputCommandGrammar.Option` on each field
- All user-facing strings via `ctx.translate(Strings.GD, "key")` or `ctx.translate(Strings.GENERAL, "key")` — add placeholder keys to the properties files

**Command with subcommands:**
- Outer class: `public final class FooCommand` with `@ChatInputCommand(name=..., subcommands={...})` — NO `@RdiService` on the outer class
- Each subcommand: `@RdiService public static final class SubName implements ChatInputInteractionListener` with its own `@RdiFactory` constructor

After generating the file, remind the user to:
1. Add any new i18n string keys to `app/src/main/resources/GDStrings.properties` (or `GeneralStrings.properties`)
2. Verify the command is picked up automatically by Botrino (no manual registration needed for `@RdiService` classes)
