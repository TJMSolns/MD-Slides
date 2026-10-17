# Contributing to MD-Slides

Thank you for your interest in contributing. This guide covers everything you need to get from zero to a merged pull request.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Architecture](#architecture)
- [Running tests](#running-tests)
- [Making a change](#making-a-change)
- [Code conventions](#code-conventions)
- [Submitting a pull request](#submitting-a-pull-request)
- [Decision records](#decision-records)

---

## Prerequisites

- **Java 11 or higher** (Java 17+ recommended)
- **Mill** build tool
- Git

You do not need to have Scala installed separately — Mill downloads the right version automatically.

### Installing Mill

**macOS / Linux:**
```bash
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.11.6/0.11.6 > mill
chmod +x mill
sudo mv mill /usr/local/bin/mill
```

**Windows:** Download the `.bat` launcher from the [Mill releases page](https://github.com/com-lihaoyi/mill/releases/tag/0.11.6) and add it to your PATH.

Verify:
```bash
mill --version
# Mill Build Tool version 0.11.6
```

---

## Getting started

```bash
git clone https://github.com/TJMSolns/MD-Slides.git
cd MD-Slides

# Compile all modules
mill __.compile

# Run all tests
mill __.test
```

First compile will download dependencies (~30s on a fresh machine). Subsequent runs are fast.

---

## Project structure

```
MD-Slides/
├── domain/                  # Pure functional domain model — no I/O
│   ├── src/                 # Aggregates, value objects, validation
│   └── test/                # Example-based + property-based tests
├── infrastructure/          # I/O adapters — parsing, rendering, file ops
│   ├── src/
│   │   └── .../
│   │       ├── parser/      # Markdown → domain model (Flexmark ACL)
│   │       ├── renderer/    # Domain model → HTML (Scalatags)
│   │       ├── theme/       # JSON theme loading (Circe)
│   │       └── assets/      # Image asset copying
│   └── test/
├── cli/                     # CLI entry point — wires domain + infrastructure
│   └── src/Main.scala
├── templates/               # Slide template definitions (YAML)
├── themes/                  # Built-in themes (JSON)
│   ├── light/
│   └── dark/
├── features/                # BDD feature specs (plain language)
├── examples/                # Example presentations
├── docs/decisions/          # Architecture and product decision records
│   ├── adr/                 # Architecture Decision Records
│   ├── pdr/                 # Product Decision Records
│   └── pol/                 # Coding policies
└── build.sc                 # Mill build file
```

---

## Architecture

MD-Slides is a three-layer application. The rule is strict: **dependencies only flow downward**.

```
┌──────────────────────────────────────────┐
│  cli                                     │
│  Cats Effect IOApp, Decline arg parsing  │
│  Wires domain + infrastructure. No logic.│
└────────────────────┬─────────────────────┘
                     │ depends on
┌────────────────────▼─────────────────────┐
│  infrastructure                          │
│  All I/O lives here.                     │
│  - MarkdownParser  (Flexmark ACL)        │
│  - HTMLRenderer    (Scalatags)           │
│  - ThemeLoader     (Circe)               │
│  - AssetCopier     (os-lib)              │
└────────────────────┬─────────────────────┘
                     │ depends on
┌────────────────────▼─────────────────────┐
│  domain                                  │
│  Pure functions only. No I/O, ever.      │
│  - Slide, SlideDeck  (aggregates)        │
│  - Template, Theme   (value objects)     │
│  - ValidationError   (error model)       │
│  - Validation pipeline                   │
└──────────────────────────────────────────┘
```

### Domain layer rules

The domain is a pure functional core. These rules are enforced by `-Xfatal-warnings` and module dependency isolation:

- No `cats-effect`, no `os-lib`, no file I/O of any kind
- Validation uses `Either[NonEmptyList[ValidationError], A]` — all errors collected, none swallowed
- Illegal states are unrepresentable: smart constructors, opaque types, sealed hierarchies
- Every public type is tested with both example-based and property-based tests

See [`docs/decisions/pol/POL-003-pure-functional-domain.md`](docs/decisions/pol/POL-003-pure-functional-domain.md) for the full rationale.

### Infrastructure layer rules

- All file I/O and external library calls live here
- Third-party libraries (Flexmark, Circe) are wrapped behind domain-facing interfaces — never leaked into domain types
- The Flexmark adapter is an anticorruption layer: it translates Flexmark's AST into MD-Slides domain types

See [`docs/decisions/adr/ADR-007-anticorruption-layer.md`](docs/decisions/adr/ADR-007-anticorruption-layer.md).

---

## Running tests

```bash
# All modules
mill __.test

# Single module
mill domain.test
mill infrastructure.test
mill cli.test

# Single suite
mill domain.test.testOnly com.tjmsolutions.mdslides.domain.SlideSpec

# With verbose output
mill domain.test + --verbosity 2
```

### Test types

**Example-based** (in `*Spec.scala`): verify concrete scenarios against known inputs and outputs.

**Property-based** (in `*Properties.scala`): use ScalaCheck to verify invariants across thousands of generated inputs. Every domain invariant must have a property-based test — see [`docs/decisions/pol/POL-004-property-based-testing.md`](docs/decisions/pol/POL-004-property-based-testing.md).

Example:
```scala
property("validated body never exceeds 12 lines") {
  forAll(validContentSlideGen) { slide =>
    Slide.validated(slide.id, slide.templateName, slide.slots) match
      case Right(validSlide) =>
        validSlide.getSlot("body").forall(b => SlotContent(b).lineCount <= 12)
      case Left(_) => true
  }
}
```

---

## Making a change

### Adding a new feature

1. **Check the feature specs** in `features/` — there may already be a `.feature` file describing the behaviour.
2. **Write a failing test first** — example-based in `*Spec.scala`, property-based in `*Properties.scala`.
3. **Implement the minimum to make it pass.**
4. **Refactor** once green.
5. **Update `CHANGELOG.md`** under `[Unreleased]`.

### Adding a new slide template

1. Add a YAML definition to `templates/`.
2. Add domain types and validation rules in `domain/`.
3. Add rendering in `infrastructure/renderer/`.
4. Add parser support in `infrastructure/parser/`.
5. Add a `.feature` file in `features/`.
6. Add tests at every layer.

### Adding a new theme

Themes are JSON files — no code change required. To add one as a built-in:

1. Create `themes/mytheme/theme.json` (see [`docs/decisions/pdr/PDR-007-theme-schema.md`](docs/decisions/pdr/PDR-007-theme-schema.md) for the schema).
2. Add the theme name constant to `Theme.scala` in `domain/`.
3. Add the loader case in `cli/Main.scala`.
4. Add tests to `ThemeSpec.scala`.

### Building the assembly JAR

```bash
mill cli.assembly

# JAR is at:
out/cli/assembly.dest/out.jar

# Test it
java -jar out/cli/assembly.dest/out.jar render examples/hello-world --theme light
```

---

## Code conventions

- **Scala 3** — use enums, opaque types, extension methods, and `given`/`using` where they reduce boilerplate
- **No shared mutable state** — immutable data structures throughout
- **`-Xfatal-warnings`** — the build will fail on any warning; fix the root cause
- **Test names use domain language** — `"validated slide has required title slot"`, not `"test1"`
- **Banned identifiers in domain code**: `Manager`, `Service`, `Handler`, `DTO` — see [`docs/decisions/pol/POL-002-banned-terms.md`](docs/decisions/pol/POL-002-banned-terms.md)
- **No comments explaining what the code does** — well-named types and functions are the documentation; comments are for non-obvious constraints and invariants only

---

## Submitting a pull request

1. Fork the repo and create a branch from `main`.
2. Make your changes following the conventions above.
3. Run `mill __.test` — all tests must pass.
4. Open a PR against `main`. Use the PR template.
5. A maintainer will review. We aim to respond within a few business days.

Small, focused PRs are much easier to review than large ones. When in doubt, open an issue first to discuss the approach.

---

## Decision records

Every non-trivial design choice is recorded in `docs/decisions/`. Before changing something fundamental (validation strategy, rendering approach, theme schema), read the relevant records — they explain not just what was decided but why alternatives were ruled out.

| Directory | Purpose |
|-----------|---------|
| `docs/decisions/adr/` | Architecture decisions — technology choices, module boundaries, patterns |
| `docs/decisions/pdr/` | Product decisions — validation limits, accessibility requirements, UX behaviour |
| `docs/decisions/pol/` | Coding policies — invariants all contributors must uphold |

If your change requires a new decision record, add it in the appropriate directory and reference it from your PR description.
