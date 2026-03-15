---
description: Rules for the util/ subpackage
globs: ["**/util/**/*.java"]
---

# Utility Package Rules

The `ultimategdbot.util` package is for reusable static helper functions shared across the codebase. It has no strict
class structure requirements — unlike services or commands, util classes are free-form.

## Guidelines

- Util classes should contain only `static` methods and a private constructor (no instantiation).
- They do not use `@RdiService` or `@RdiFactory` — they are not DI-managed.
- Place any formatting helpers, embed templates, Discord interaction utilities, or other cross-cutting pure functions
  here.
- Reactive helpers that return `Mono`/`Flux` are welcome here if they are reusable across multiple commands or services.
