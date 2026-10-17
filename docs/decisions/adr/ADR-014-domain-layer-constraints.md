# ADR-014: Domain Layer Dependency Constraint and Effect System Exception

**Status:** Accepted  
**Date:** 2026-05-17  
**Supercedes:** —  
**Author:** Tony Moores / Claude

---

## Context

mdslides is a pure-functional Scala 3 CLI tool with a three-module layered architecture:
`domain` → `infrastructure` → `cli`.

Two constraints were enforced from the beginning but never formally recorded:

1. **The domain layer must remain dependency-free except for Cats Core.** The `domain` module contains all business logic — slide aggregates, validation, slot/template model, FormattedContent — and must be zero I/O, zero side effects.

2. **mdslides uses Cats Effect, not ZIO.** The TJM Solutions org-wide default is ZIO 2 (established 2026). mdslides predates this standard and was built with Cats Effect. It has 300+ tests wired against the Cats Effect runtime.

This ADR formalizes both constraints so they are traceable from the CONTEXT-KERNEL non-negotiables.

---

## Decisions

### 1. Domain layer must never depend on Cats Effect or os-lib

The `domain` Mill module's `ivyDeps` must contain only Cats Core (for typeclasses) and Cats Data (for `NonEmptyList`). It must never acquire:

- `cats-effect` or any `IO`-bearing library
- `os-lib` or any filesystem library
- Any HTTP, database, or serialization library

Violations turn the domain into an I/O-capable module, couple business logic to a runtime, and undermine property-based testability.

### 2. Cats Effect is an approved and permanent exception to the org ZIO standard

All I/O in mdslides runs in the `infrastructure` and `cli` modules via Cats Effect. This is a permanent architectural choice — no migration to ZIO is planned or warranted.

New contributors who notice "this project doesn't use ZIO" should read this ADR rather than treating it as a gap.

---

## Rationale

**For the domain constraint:**
- Pure domain module → logic is testable with `ScalaCheck` and `MUnit` without runtime setup or mocking
- The constraint is enforced in Mill by the module dependency graph; `domain` cannot compile against `cats-effect` unless it is explicitly added to `ivyDeps`
- ADR-007 (pure functional domain) documents the domain design philosophy; this ADR is its enforcement mechanism

**For the Cats Effect exception:**
- mdslides is a single-process CLI tool — neither ZIO's multi-region fibers nor its typed error channel provide material benefit over Cats Effect for this use case
- Migrating infrastructure + cli + all tests to ZIO would produce zero user-visible value and break the existing 300+ test suite
- Cats Effect provides the same semantic guarantees: pure FP, referential transparency, structured concurrency for the one async step (Mermaid pre-render)

---

## Consequences

- Any change that adds `cats-effect` or `os-lib` to `domain/build.sc` `ivyDeps` must be rejected
- Infrastructure and CLI modules may freely use Cats Effect idioms; `IO` is the effect type throughout
- When org-wide ZIO requirements are cited against mdslides, this ADR is the authoritative reference
- Any future mdslides contributor should be directed to this ADR before suggesting a ZIO migration

---

## Alternatives Rejected

| Alternative | Why Rejected |
|-------------|-------------|
| Migrate domain to ZIO effect types | Pure domain logic is synchronous; ZIO's effect type adds ceremony with no benefit in a zero-I/O module |
| Migrate infrastructure + CLI to ZIO | Significant churn; Cats Effect is fully adequate for a single-process CLI; no user-visible benefit |
| Allow ad-hoc I/O in domain for convenience | Breaks property-based testability; once domain acquires I/O dependencies, the guarantee of purity is gone |
