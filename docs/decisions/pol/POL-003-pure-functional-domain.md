# POL-003: Pure Functional Domain

**Status**: Active
**Date**: 2024-12-20
**Owners**: Tony Moores (Architect, Product Owner)

---

## Policy Statement

The domain layer (`domain/` module) MUST be pure functional - no side effects, no I/O operations, no mutable state. All domain functions must be referentially transparent (same input → same output, always).

Code that violates this policy (performs I/O, mutates state, or has non-deterministic behavior) will be rejected in code review.

---

## Rationale

Pure functional domain models provide critical benefits:

1. **Testability**: Pure functions trivial to test (no mocking, no setup, no cleanup)
2. **Determinism**: Same input always produces same output (reproducible bugs)
3. **Parallelization**: Pure functions safe to run in parallel (no race conditions)
4. **Reasoning**: Easy to understand behavior (no hidden state, no side effects)
5. **Refactoring**: Safe to extract/inline pure functions (behavior preserved)
6. **Type Safety**: Effects explicit in types (`IO`, `Either`)

**DDD Alignment**: Domain-Driven Design emphasizes that the domain layer should contain pure business logic, with infrastructure concerns (I/O, databases, frameworks) isolated in separate layers.

**Problem This Policy Prevents**:
- Untestable domain logic (requires database, file system, network)
- Non-deterministic bugs (behavior depends on external state)
- Thread-safety issues (mutable shared state)
- Tight coupling (domain depends on infrastructure)

---

## Scope

**Applies to**:
- All code in `domain/` module
- Domain model classes (entities, value objects, aggregates)
- Domain logic (validation, transformations, calculations)
- Domain services (if needed - should be rare)

**Does NOT apply to**:
- Infrastructure layer (`infrastructure/` module) - I/O allowed
- CLI layer (`cli/` module) - effects allowed (Cats Effect `IO`)
- Test fixtures (can use I/O for test data setup)

---

## Enforcement

### Automated Checks

**v1.0** (Manual enforcement):
- Code review only

**v1.1+** (Future automation):
- ArchUnit rules (if ported to Scala)
- Custom Scalafix rule detecting forbidden imports in domain layer:
  - `java.io._`
  - `scala.io._`
  - `cats.effect.IO` (not allowed in domain)
  - `System.out.println`
  - `scala.util.Random` (non-deterministic)

### Code Review Checklist

Reviewers MUST verify:
- [ ] No I/O operations in domain layer (file read/write, network, database)
- [ ] No `println`, `System.out`, `System.err` (side effects)
- [ ] No mutable state (`var`, mutable collections)
- [ ] No `IO` monad in domain signatures (keep effects at edges)
- [ ] No non-deterministic operations (`Random`, `System.currentTimeMillis`)
- [ ] All domain functions referentially transparent (same input → same output)

### Example Review Comments

❌ **REJECT**:
```scala
// domain/src/.../SlideDeck.scala
case class SlideDeck(slides: List[Slide]) {
  def save(filename: String): Unit = {
    val writer = new PrintWriter(filename)  // I/O in domain!
    writer.write(this.toJson)
    writer.close()
  }
}
```
**Review comment**: "Domain layer must be pure. Move `save` to infrastructure layer (e.g., `SlideDeckWriter` in `infrastructure/`). Domain should only provide `toJson: String` method."

❌ **REJECT**:
```scala
// domain/src/.../Slide.scala
case class Slide(id: Int, template: String, slots: Map[String, String]) {
  def validate(): Either[ValidationError, Slide] = {
    println(s"Validating slide $id")  // Side effect!
    // ... validation logic
  }
}
```
**Review comment**: "Domain layer must not have side effects. Remove `println`. If logging needed, use structured logging in CLI/infrastructure layer."

❌ **REJECT**:
```scala
// domain/src/.../SlideId.scala
case class SlideId(value: Int)

object SlideId {
  def generate(): SlideId = {
    SlideId(scala.util.Random.nextInt())  // Non-deterministic!
  }
}
```
**Review comment**: "Domain layer must be deterministic. Move ID generation to infrastructure layer. Domain should accept pre-generated IDs."

✅ **APPROVE**:
```scala
// domain/src/.../SlideDeck.scala
case class SlideDeck(slides: List[Slide]) {
  def toJson: String = {  // Pure (returns String)
    // JSON serialization logic
    s"""{"slides": [...]}"""
  }

  def validate(templates: Map[String, Template]): Either[NonEmptyList[ValidationError], SlideDeck] = {
    // Pure validation (no side effects)
    val errors = slides.flatMap(slide => validateSlide(slide, templates))
    NonEmptyList.fromList(errors) match {
      case Some(errs) => Left(errs)
      case None       => Right(this)
    }
  }
}
```

---

## What is Pure?

### Pure Function Definition
A function `f: A => B` is pure if:
1. **Deterministic**: `f(a) == f(a)` always (same input → same output)
2. **No side effects**: Only computes result, doesn't modify external state
3. **Referentially transparent**: Can replace `f(a)` with its result everywhere

### Examples of Pure Functions

✅ **Pure** (allowed in domain):
```scala
// Mathematical calculations
def calculateTotalSlides(deck: SlideDeck): Int = deck.slides.length

// Data transformations
def filterEmptySlides(deck: SlideDeck): SlideDeck = {
  deck.copy(slides = deck.slides.filter(_.slots.nonEmpty))
}

// Validation (returns Either)
def validateStructure(slide: Slide, template: Template): Either[StructureError, Slide] = {
  if (slide.getSlot("title").isEmpty) {
    Left(StructureError(slide.id, "Missing title"))
  } else {
    Right(slide)
  }
}

// String formatting
def formatError(error: ValidationError): String = {
  s"Slide ${error.slideId}: ${error.message}"
}
```

### Examples of Impure Functions

❌ **Impure** (NOT allowed in domain):
```scala
// I/O operations
def loadSlide(filename: String): Slide = {
  val source = scala.io.Source.fromFile(filename)  // I/O!
  // ...
}

// Console output
def logValidation(slide: Slide): Slide = {
  println(s"Validating ${slide.id}")  // Side effect!
  slide
}

// Mutable state
class SlideCounter {
  private var count = 0  // Mutable state!
  def increment(): Unit = { count += 1 }
}

// Non-deterministic
def generateSlideId(): SlideId = {
  SlideId(scala.util.Random.nextInt())  // Non-deterministic!
}

// Current time
def createTimestamp(): Long = {
  System.currentTimeMillis()  // Non-deterministic!
}
```

---

## Where Do Effects Go?

Effects (I/O, state, non-determinism) belong in **infrastructure** and **CLI** layers.

### Architecture Layers

```
┌─────────────────────────────────────────────┐
│ CLI Layer (cats.effect.IO)                  │
│ - File I/O (read markdown, write HTML)      │
│ - Console I/O (println, error messages)     │
│ - Exit codes (sys.exit)                     │
│ - Effect orchestration (for-comprehension)  │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│ Infrastructure Layer (adapters)              │
│ - MarkdownParser (Flexmark I/O)             │
│ - ThemeLoader (file reading, Circe)         │
│ - HtmlWriter (file writing)                 │
│ - Random ID generation                       │
└─────────────┬───────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────┐
│ Domain Layer (PURE - no effects)             │
│ - SlideDeck, Slide, Template, Theme          │
│ - Validation (pure functions)                │
│ - Rendering logic (pure HTML generation)     │
│ - Transformations (pure data operations)     │
└─────────────────────────────────────────────┘
```

### Example: Proper Layering

```scala
// ✅ Domain Layer (pure)
// domain/src/.../SlideDeck.scala
case class SlideDeck(slides: List[Slide]) {
  def validate(templates: Map[String, Template]): Either[NonEmptyList[ValidationError], SlideDeck] = {
    // Pure validation logic
    ???
  }

  def toJson: String = {
    // Pure serialization
    s"""{"slides": [...]}"""
  }
}

// ✅ Infrastructure Layer (I/O allowed)
// infrastructure/src/.../SlideDeckWriter.scala
class SlideDeckWriter {
  def write(deck: SlideDeck, filename: String): IO[Unit] = {
    IO {
      val writer = new PrintWriter(filename)
      writer.write(deck.toJson)  // Calls pure domain method
      writer.close()
    }
  }
}

// ✅ CLI Layer (orchestrates effects)
// cli/src/.../Main.scala
def main(args: Array[String]): Unit = {
  val program = for {
    markdown  <- readFile("input.md")           // IO[String]
    deck      =  parseSlideDeck(markdown)       // Pure (SlideDeck)
    validated <- IO.fromEither(deck.validate(templates))  // Either → IO
    _         <- writeDeck(validated, "output.json")  // IO[Unit]
  } yield ()

  program.unsafeRunSync()  // Run effects
}
```

---

## Handling Effects

### Pattern 1: Return Values (Not Side Effects)

❌ **Bad** (side effect):
```scala
def renderSlide(slide: Slide): Unit = {
  println(s"<div>${slide.title}</div>")  // Side effect!
}
```

✅ **Good** (return value):
```scala
def renderSlide(slide: Slide): Html = {
  div(slide.title)  // Returns value (infrastructure layer prints it)
}
```

### Pattern 2: Either for Errors (Not Exceptions)

❌ **Bad** (exceptions are side effects):
```scala
def validateSlide(slide: Slide): Slide = {
  if (slide.getSlot("title").isEmpty) {
    throw new ValidationException("Missing title")  // Side effect!
  }
  slide
}
```

✅ **Good** (Either for errors):
```scala
def validateSlide(slide: Slide): Either[ValidationError, Slide] = {
  if (slide.getSlot("title").isEmpty) {
    Left(StructureError(slide.id, "Missing title"))
  } else {
    Right(slide)
  }
}
```

### Pattern 3: Accept Pre-Generated Values (Not Generate)

❌ **Bad** (non-deterministic generation):
```scala
case class Slide(id: SlideId, template: String, slots: Map[String, String])

object Slide {
  def create(template: String): Slide = {
    Slide(
      id = SlideId(Random.nextInt()),  // Non-deterministic!
      template = template,
      slots = Map.empty
    )
  }
}
```

✅ **Good** (accept pre-generated ID):
```scala
case class Slide(id: SlideId, template: String, slots: Map[String, String])

object Slide {
  def create(id: SlideId, template: String): Slide = {
    Slide(id, template, Map.empty)  // ID generated elsewhere
  }
}

// Infrastructure layer generates IDs
class SlideIdGenerator {
  def generate(): IO[SlideId] = IO(SlideId(Random.nextInt()))
}
```

---

## Testing Pure Functions

Pure functions are trivial to test:

```scala
class SlideDeckValidatorSpec extends munit.FunSuite {
  test("missing title slot fails validation") {
    // Setup (pure data construction)
    val slide = Slide(
      id = SlideId(1),
      template = "title",
      slots = Map.empty  // Missing title
    )
    val deck = SlideDeck(List(slide))
    val templates = Map("title" -> Template(...))

    // Exercise (pure function call)
    val result = deck.validate(templates)

    // Verify (pure assertion)
    assert(result.isLeft)
    assertEquals(result.left.get.head.message, "Missing required slot 'title'")
  }
}
```

**No mocking, no setup, no cleanup, no flakiness.**

---

## Exceptions

### Exception 1: Logging in Domain Tests
**Allowed**: `println` in test code for debugging (not production domain code)
```scala
test("complex validation") {
  val result = validateSlideDeck(deck)
  println(s"Debug: $result")  // OK in tests
  assert(result.isRight)
}
```

### Exception 2: ArchUnit Testing (Future)
If we add ArchUnit-style tests, they will inherently use reflection (side effect). This is acceptable in test code.

**Approval Process**: Architect must approve exceptions via code review comment.

---

## Forbidden Imports in Domain Layer

The following imports indicate impurity and are BANNED from `domain/`:

```scala
// ❌ File I/O
import java.io._
import scala.io.Source
import java.nio.file._

// ❌ Effects
import cats.effect.IO
import cats.effect.unsafe.IORuntime

// ❌ Console I/O
// (No import, but System.out.println forbidden)

// ❌ Non-determinism
import scala.util.Random
import java.util.UUID
import java.time.Instant  // System.currentTimeMillis

// ❌ Mutable collections
import scala.collection.mutable._
```

**Allowed imports** (pure):
```scala
// ✅ Immutable data structures
import cats.data.NonEmptyList
import cats.data.Validated

// ✅ Type classes (pure)
import cats.Monad
import cats.syntax.all._

// ✅ Immutable collections
import scala.collection.immutable._
```

---

## Related Policies

- **POL-001**: Ubiquitous Language Enforcement
- **POL-002**: Banned Terms in Domain Layer

---

## Related ADRs

- **ADR-007**: Pure Functional Domain Model (architectural decision behind this policy)

---

## Related Artifacts

- "Functional Programming in Scala" by Chiusano & Bjarnason (Red Book)
- `doc/domain-models/` - Domain models (all pure)

---

**Policy Owner**: Tony Moores (Architect)
**Enforcement**: Code review (manual), future Scalafix rules (automated)
**Next Review**: 2025-03-20 (quarterly)
