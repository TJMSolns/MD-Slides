# ADR-001: Technology Stack Selection

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-016 (HTML Rendering), US-019 (CLI Interface), US-008 (Theme System)

---

## Context

MDSlides requires a robust technology stack for:
- **Markdown Parsing**: Convert Markdown → AST with structure preservation
- **HTML Generation**: Type-safe HTML templating with CSS/JS inlining
- **CLI Parsing**: Command-line argument parsing with help/version support
- **JSON Parsing**: Theme file deserialization with validation
- **Functional Effects**: Pure functional domain with controlled side effects
- **Type Safety**: Prevent runtime errors through compile-time checking

The stack must support:
- Pure functional programming (DDD principle: pure domain layer)
- Type safety (prevent template mismatches, invalid states)
- Maintainability (clear code, good error messages)
- Performance (< 500ms rendering for 50-slide deck)

---

## Decision

**Core Language**: Scala 3.3.1

**Libraries**:
1. **Scalatags** (HTML generation)
   - Type-safe HTML DSL
   - Compile-time tag/attribute validation
   - Easy CSS/JS inlining for standalone output

2. **Flexmark** (Markdown parsing)
   - Proven CommonMark parser with extensions
   - AST-based (not regex-based)
   - Support for GFM (GitHub Flavored Markdown)
   - Extensions: tables, strikethrough, task lists

3. **Decline** (CLI parsing)
   - Functional CLI parser (pure, composable)
   - Automatic help generation
   - Type-safe argument parsing
   - Integrates with Cats Effect

4. **Circe** (JSON parsing)
   - Functional JSON library
   - Automatic codec derivation
   - Good error messages
   - Integrates with Cats

5. **Cats Effect** (Functional effects)
   - Pure functional IO
   - Resource safety (automatic file closing)
   - Error handling (MonadError)
   - Referential transparency

6. **Mill** (Build tool)
   - Scala-based build definitions
   - Fast incremental compilation
   - Simple module system

---

## Consequences

### Positive

1. **Type Safety**: Scalatags prevents invalid HTML, Scala 3 prevents template mismatches
2. **Pure Domain**: Cats Effect isolates side effects to infrastructure layer
3. **Proven Libraries**: Flexmark used by major projects (IntelliJ, GitLab)
4. **Maintainability**: Functional code easier to test, reason about
5. **Error Messages**: Circe/Decline provide actionable error messages
6. **Standalone Output**: Scalatags makes CSS/JS inlining trivial

### Negative

1. **Learning Curve**: Scala 3 + Cats Effect requires functional programming knowledge
2. **Binary Size**: JVM + libraries = larger binary than Go/Rust alternatives
3. **Startup Time**: JVM startup ~100-200ms (acceptable for CLI tool)
4. **Ecosystem Size**: Smaller than JavaScript/Python ecosystems

### Risks

1. **Risk**: Flexmark updates breaking parsing behavior
   - **Mitigation**: Pin version, comprehensive test suite
2. **Risk**: Scalatags maintenance (smaller community)
   - **Mitigation**: Simple library, could fork if needed
3. **Risk**: Team unfamiliar with Cats Effect
   - **Mitigation**: Document patterns, limit effects to infrastructure layer

---

## Alternatives Considered

### Alternative A: Python + Jinja2 + Click
**Why Rejected**:
- Lack of type safety (runtime template errors)
- No compile-time validation
- Slower execution for large decks

### Alternative B: TypeScript + React + Commander
**Why Rejected**:
- React overkill for static HTML generation
- Node.js dependency for CLI tool
- No strong domain modeling support

### Alternative C: Rust + Tera + clap
**Why Rejected**:
- Steeper learning curve for team
- Less expressive domain modeling
- Build complexity for HTML generation

### Alternative D: Haskell + Blaze + optparse-applicative
**Why Rejected**:
- Even steeper learning curve
- Smaller ecosystem
- Harder to hire for

---

## Implementation Notes

### Module Structure
```
domain/       (Pure Scala, no dependencies on Flexmark/Scalatags)
  ├─ models/  (SlideDeck, Slide, Template, Theme)
  ├─ validation/
  └─ rendering/

infrastructure/
  ├─ parser/  (Flexmark integration)
  ├─ theme/   (Circe integration)
  └─ html/    (Scalatags integration)

cli/          (Decline + Cats Effect)
```

### Dependency Injection
Use constructor-based DI (no framework needed):
```scala
class SlideDeckRenderer(
  parser: MarkdownParser,
  validator: SlideDeckValidator,
  themeLoader: ThemeLoader,
  htmlGenerator: HtmlGenerator
)
```

### build.sc Configuration
```scala
object domain extends ScalaModule {
  def scalaVersion = "3.3.1"
  def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:2.10.0",
    ivy"org.typelevel::cats-effect:3.5.2"
  )
}

object infrastructure extends ScalaModule {
  def moduleDeps = Seq(domain)
  def ivyDeps = Agg(
    ivy"com.vladsch.flexmark:flexmark-all:0.64.8",
    ivy"com.lihaoyi::scalatags:0.12.0",
    ivy"io.circe::circe-core:0.14.6",
    ivy"io.circe::circe-generic:0.14.6",
    ivy"io.circe::circe-parser:0.14.6"
  )
}

object cli extends ScalaModule {
  def moduleDeps = Seq(domain, infrastructure)
  def ivyDeps = Agg(
    ivy"com.monovore::decline:2.4.1",
    ivy"com.monovore::decline-effect:2.4.1"
  )
}
```

### Version Pinning Strategy
- **Scala 3.3.1**: LTS release (stable API)
- **Flexmark 0.64.8**: Pinned (parsing stability critical)
- **Scalatags 0.12.0**: Pinned (HTML generation stability)
- **Cats Effect 3.5.x**: Patch updates allowed (backwards compatible)

---

**ADR Type**: Technical Foundation
**Impact**: Project-wide (all modules affected)
**Reversibility**: Low (requires major refactoring)
**Validation**: Proven in Phase 2 ceremonies (all 12 stories validated against this stack)
