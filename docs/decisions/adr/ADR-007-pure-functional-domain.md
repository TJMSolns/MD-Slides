# ADR-007: Pure Functional Domain Model

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: All ceremonies (foundational decision)

---

## Context

Domain-Driven Design (DDD) emphasizes a rich, expressive domain model that reflects the ubiquitous language. For MDSlides, this includes:
- **Entities**: SlideDeck, Slide, Template, Theme
- **Value Objects**: SlideId, SlotContent, ValidationError
- **Behaviors**: Validation, rendering, parsing

**Key Question**: Should the domain layer be pure (no side effects) or pragmatic (with I/O)?

**Constraints**:
- **Testability**: Domain logic must be easily testable
- **Referential Transparency**: Same input → same output (deterministic)
- **Type Safety**: Prevent invalid states at compile time
- **Maintainability**: Clear separation between business logic and infrastructure

---

## Decision

**Domain layer MUST be pure** - no side effects, no I/O, referential transparency:

```scala
// ✅ Pure domain functions (allowed)
def validateSlide(slide: Slide): Either[NonEmptyList[ValidationError], Slide]
def renderSlide(slide: Slide, theme: Theme): Html

// ❌ Impure domain functions (NOT allowed)
def loadSlide(slideId: SlideId): IO[Slide]  // I/O in domain
def saveSlide(slide: Slide): Unit            // Side effect in domain
```

**Architecture**:
```
┌─────────────────────────────────────────────┐
│ CLI Layer (Cats Effect IO)                 │
│ - File I/O                                  │
│ - Error handling                            │
│ - Effects orchestration                     │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│ Infrastructure Layer (Adapters)             │
│ - MarkdownParser (Flexmark → AST)          │
│ - ThemeLoader (Circe → Theme)              │
│ - HtmlGenerator (Scalatags → HTML)         │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│ Domain Layer (Pure Functions)               │
│ - SlideDeck, Slide, Template, Theme         │
│ - Validation (Structure, Density, Content)  │
│ - Domain logic (PURE - no side effects)     │
└─────────────────────────────────────────────┘
```

**Cats Effect for Effects**:
- Use `IO` monad for side effects (file I/O, console output)
- Keep effects at the edges (CLI, infrastructure)
- Domain functions return pure values or `Either`

---

## Consequences

### Positive

1. **Testability**: Pure functions trivial to test (no mocking, no setup)
2. **Deterministic**: Same input always produces same output
3. **Parallelizable**: Pure functions safe to run in parallel (no race conditions)
4. **Composable**: Pure functions compose like LEGO blocks
5. **Reasoning**: Easy to understand behavior (no hidden state)
6. **Refactoring**: Safe to extract/inline pure functions
7. **Type Safety**: Effects explicit in types (`IO`, `Either`)

### Negative

1. **Learning Curve**: Team must understand functional programming (IO monad, Either, etc.)
2. **Verbosity**: Explicit effect handling more verbose than imperative code
3. **Ecosystem**: Smaller Scala FP ecosystem than imperative Scala

### Risks

1. **Risk**: Team unfamiliar with Cats Effect
   - **Mitigation**: Document patterns, keep effects in CLI layer only
2. **Risk**: Mixing pure and impure code
   - **Mitigation**: POL-003 policy, code review enforcement

---

## Alternatives Considered

### Alternative A: Pragmatic Domain (Allow I/O)
```scala
class SlideDeckRepository {
  def load(file: String): SlideDeck = {
    val markdown = readFile(file)  // I/O in domain
    parse(markdown)
  }
}
```
**Why Rejected**:
- Hard to test (requires file system, mocking)
- Non-deterministic (depends on external state)
- Violates DDD principle (domain should be pure business logic)

### Alternative B: ZIO Instead of Cats Effect
```scala
import zio._

def validateSlide(slide: Slide): IO[ValidationError, Slide]
```
**Why Rejected**:
- Cats Effect more established in Scala ecosystem
- ZIO has steeper learning curve
- Not worth switching for v1.0 (revisit in v2.0)

### Alternative C: Tagless Final
```scala
trait SlideDeckService[F[_]] {
  def validate(deck: SlideDeck): F[Either[ValidationError, SlideDeck]]
}
```
**Why Rejected**:
- Over-engineering for v1.0
- Adds complexity without clear benefit
- Harder for team to understand

### Alternative D: Impure Domain + ArchUnit Enforcement
```scala
// Allow impure domain, enforce via ArchUnit rules
```
**Why Rejected**:
- ArchUnit is Java-focused (Scala support limited)
- Runtime enforcement weaker than compile-time
- Doesn't prevent accidents during development

---

## Implementation Notes

### Pure Domain Examples

```scala
// ✅ Pure validation
def validateStructure(deck: SlideDeck): Either[NonEmptyList[StructureError], SlideDeck] = {
  val errors = deck.slides.flatMap { slide =>
    slide.template match {
      case "title" =>
        if (slide.getSlot("title").isEmpty) {
          List(StructureError(slide.id, "Missing required slot 'title'"))
        } else Nil
      case _ => Nil
    }
  }

  NonEmptyList.fromList(errors) match {
    case Some(errs) => Left(errs)
    case None       => Right(deck)
  }
}

// ✅ Pure rendering (returns value, no I/O)
def renderSlide(slide: Slide, theme: Theme): Html = {
  div(cls := "slide")(
    h1(slide.getSlot("title").getOrElse(""))
  )
}

// ✅ Pure transformation
def applyTheme(deck: SlideDeck, theme: Theme): SlideDeck = {
  deck.copy(metadata = deck.metadata.copy(themeName = Some(theme.name)))
}
```

### Effects at the Edges

```scala
import cats.effect.IO

// ❌ NOT in domain layer
// ✅ In CLI layer
def loadAndValidate(inputFile: String): IO[Either[ValidationError, SlideDeck]] = {
  for {
    markdown <- readFile(inputFile)           // I/O effect
    deck     <- IO.pure(parseMarkdown(markdown))  // Pure wrapped in IO
    result   <- IO.pure(validateStructure(deck))  // Pure wrapped in IO
  } yield result
}

// Pure domain function
def parseMarkdown(markdown: String): SlideDeck = {
  // Parse logic (deterministic, no side effects)
}
```

### Testing Pure Functions

```scala
class ValidationSpec extends munit.FunSuite {
  test("missing title slot fails validation") {
    // No setup needed, no mocking
    val slide = Slide(
      id = 1,
      template = "title",
      slots = Map.empty  // Missing title
    )
    val deck = SlideDeck(List(slide))

    val result = validateStructure(deck)

    assert(result.isLeft)
    assertEquals(result.left.get.head.message, "Missing required slot 'title'")
  }
}
```

### Dependency Injection (Pure)

```scala
// Constructor-based DI (no framework)
class SlideDeckService(
  parser: String => SlideDeck,               // Pure function
  validator: SlideDeck => Either[Error, SlideDeck],  // Pure function
  renderer: (SlideDeck, Theme) => Html       // Pure function
) {
  def process(markdown: String, theme: Theme): Either[Error, Html] = {
    for {
      deck      <- Right(parser(markdown))
      validated <- validator(deck)
      html      <- Right(renderer(validated, theme))
    } yield html
  }
}

// Usage (no framework, no annotations)
val service = new SlideDeckService(
  parser = parseMarkdown,
  validator = validateStructure,
  renderer = renderSlideDeck
)
```

### Effect Composition

```scala
import cats.effect.IO
import cats.syntax.all._

def pipeline(
  inputFile: String,
  themeName: String,
  outputFile: String
): IO[Unit] = {
  for {
    markdown <- readFile(inputFile)           // IO[String]
    theme    <- loadTheme(themeName)          // IO[Theme]
    deck     =  parseMarkdown(markdown)       // Pure (SlideDeck)
    validated <- IO.fromEither(validateStructure(deck))  // Either → IO
    html     =  renderSlideDeck(validated, theme)  // Pure (Html)
    _        <- writeFile(outputFile, html.render)  // IO[Unit]
  } yield ()
}
```

### Module Structure

```
domain/src/com/tjmsolutions/mdslides/domain/
├─ model/
│  ├─ SlideDeck.scala        (Pure case class)
│  ├─ Slide.scala            (Pure case class)
│  ├─ Template.scala         (Pure case class)
│  └─ Theme.scala            (Pure case class)
├─ validation/
│  ├─ StructureValidator.scala  (Pure functions)
│  ├─ DensityValidator.scala    (Pure functions)
│  └─ ContentValidator.scala    (Pure functions)
└─ rendering/
   └─ SlideRenderer.scala       (Pure functions)

infrastructure/src/com/tjmsolutions/mdslides/infrastructure/
├─ parser/
│  └─ FlexmarkParser.scala      (Flexmark → SlideDeck)
├─ theme/
│  └─ ThemeLoader.scala         (Circe → Theme)
└─ html/
   └─ ScalatagsGenerator.scala  (SlideDeck → HTML)

cli/src/com/tjmsolutions/mdslides/cli/
└─ Main.scala                   (IO effects, CLI parsing)
```

### Forbidden in Domain Layer

```scala
// ❌ NOT ALLOWED
import java.io._
import scala.io.Source
import cats.effect.IO  // IO monad not in domain

// ❌ NOT ALLOWED
class SlideDeck {
  def save(file: String): Unit = {
    val writer = new PrintWriter(file)
    writer.write(this.toString)
    writer.close()
  }
}

// ✅ ALLOWED (pure)
class SlideDeck {
  def toJson: String = {
    // Pure serialization (no I/O)
    s"""{"slides": [...]}"""
  }
}
```

---

**ADR Type**: Architectural Principle
**Impact**: Domain layer (all domain code)
**Reversibility**: Low (fundamental architectural decision)
**Validation**: All ceremonies assume pure domain
**Key Decision Rationale**: Pure functions are testable, deterministic, composable, and align with DDD principles
**Related Policies**: POL-003 (Pure Functional Domain enforcement)
