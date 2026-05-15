# ADR-003: Error Collection Pattern

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-015 (Error Collection and Reporting)

---

## Context

When validating slide decks, multiple errors can occur:
- Multiple slides with structural issues
- Multiple density warnings in a single slide
- Multiple content constraint violations across the deck

**User Experience Problem**:
- **Fail-fast approach**: Author fixes one error, re-runs tool, sees next error (frustrating)
- **Better approach**: Show ALL errors at once so author can fix them in one pass

**Technical Challenge**:
How to collect and aggregate errors from multiple validators while preserving type safety?

---

## Decision

Use **Cats `NonEmptyList`** for error collection with `Either[NonEmptyList[E], A]` return type:

```scala
import cats.data.NonEmptyList

// Domain return type for all validators
type ValidationResult[A] = Either[NonEmptyList[ValidationError], A]

// Example
def validateSlideDeck(deck: SlideDeck): ValidationResult[SlideDeck] = {
  // Collect all errors from all slides
  val errors: List[ValidationError] = deck.slides.flatMap(validateSlide)

  NonEmptyList.fromList(errors) match {
    case Some(nel) => Left(nel)  // At least one error
    case None      => Right(deck) // No errors
  }
}
```

**Key Properties**:
1. **Type Safety**: `NonEmptyList` guarantees at least one error (can't have `Left(List.empty)`)
2. **Accumulation**: Errors collected from all slides before returning
3. **Composability**: `Either` composes with for-comprehensions, `traverse`, `parTraverse`
4. **Clear Semantics**: `Left(errors)` = failed, `Right(value)` = success

---

## Consequences

### Positive

1. **Better UX**: Authors see ALL errors in one pass
2. **Type Safety**: `NonEmptyList` prevents impossible state (`Left` with empty list)
3. **Standard Pattern**: Cats `NonEmptyList` is idiomatic in Scala functional code
4. **Composability**: Works with `traverse`, `sequence`, `parTraverse` for parallel validation
5. **Clear Intent**: Return type signals "might have multiple errors"
6. **Testing**: Easy to assert on error count, specific errors

### Negative

1. **Learning Curve**: Team must understand `NonEmptyList`, `traverse` patterns
2. **Verbosity**: More code than simple `Either[String, A]` (but more expressive)
3. **Library Dependency**: Requires Cats (but already using for effects)

### Risks

1. **Risk**: Team unfamiliar with `NonEmptyList`
   - **Mitigation**: Document pattern, provide examples in tests
2. **Risk**: Error accumulation might mask root cause (first error most important)
   - **Mitigation**: Sort errors by slide ID, preserve order

---

## Alternatives Considered

### Alternative A: Fail-Fast with `Either[String, A]`
```scala
def validate(deck: SlideDeck): Either[String, SlideDeck] = {
  deck.slides.foreach { slide =>
    if (invalid(slide)) return Left(s"Slide ${slide.id} invalid")
  }
  Right(deck)
}
```
**Why Rejected**:
- Poor UX (one error at a time)
- Doesn't leverage Scala's type system
- Early return is imperative (not functional)

### Alternative B: `Either[List[E], A]` (vanilla List)
```scala
def validate(deck: SlideDeck): Either[List[ValidationError], SlideDeck]
```
**Why Rejected**:
- Allows impossible state: `Left(List.empty)` (what does that mean?)
- No type-level guarantee of at least one error
- Confusing semantics (is `Left(Nil)` success or failure?)

### Alternative C: Custom ADT `ValidationResult`
```scala
enum ValidationResult[+E, +A] {
  case Success(value: A)
  case Failure(errors: NonEmptyList[E])
}
```
**Why Rejected**:
- Reinventing the wheel (`Either` already exists)
- Doesn't compose with for-comprehensions
- No Cats typeclass instances (Monad, Traverse, etc.)
- More code to maintain

### Alternative D: Accumulating `Validated` from Cats
```scala
import cats.data.Validated
import cats.data.ValidatedNel

def validate(deck: SlideDeck): ValidatedNel[ValidationError, SlideDeck]
```
**Why Rejected (but close)**:
- `Validated` is for **accumulation** (collects errors even if some steps succeed)
- `Either` is for **short-circuiting** (stops on first failure in for-comprehension)
- We want short-circuiting **within a validator** but accumulation **across slides**
- `Either` + `traverse` achieves this (see implementation notes)

---

## Implementation Notes

### Error ADT

```scala
sealed trait ValidationError {
  def slideId: SlideId
  def message: String
  def severity: Severity
}

enum Severity {
  case Error
  case Warning
}

case class StructureError(
  slideId: SlideId,
  message: String
) extends ValidationError {
  def severity = Severity.Error
}

case class DensityWarning(
  slideId: SlideId,
  message: String,
  actual: Int,
  limit: Int
) extends ValidationError {
  def severity = Severity.Warning
}

case class ContentError(
  slideId: SlideId,
  message: String
) extends ValidationError {
  def severity = Severity.Error
}
```

### Error Collection Pattern

```scala
import cats.data.NonEmptyList
import cats.syntax.all._

// Validate single slide (can have multiple errors)
def validateSlide(slide: Slide): List[ValidationError] = {
  val errors = List.newBuilder[ValidationError]

  // Check required slot
  if (slide.getSlot("title").isEmpty) {
    errors += StructureError(slide.id, "Missing required slot 'title'")
  }

  // Check density
  val bodyLines = slide.getSlot("body").map(_.split("\n").length).getOrElse(0)
  if (bodyLines > 12) {
    errors += DensityWarning(slide.id, "Body exceeds 12 lines", bodyLines, 12)
  }

  errors.result()
}

// Validate entire deck (accumulate errors from all slides)
def validateDeck(deck: SlideDeck): ValidationResult[SlideDeck] = {
  val allErrors: List[ValidationError] = deck.slides.flatMap(validateSlide)

  NonEmptyList.fromList(allErrors) match {
    case Some(errors) => Left(errors)
    case None         => Right(deck)
  }
}
```

### Error Formatting for CLI

```scala
def formatErrors(errors: NonEmptyList[ValidationError]): String = {
  val grouped = errors.toList.groupBy(_.slideId)
  val header = s"Validation failed with ${errors.size} error(s):\n\n"

  val formatted = grouped.toList.sortBy(_._1).map { case (slideId, errs) =>
    val slideHeader = s"Slide $slideId:"
    val slideErrors = errs.map { err =>
      val prefix = err.severity match {
        case Severity.Error   => "  ❌"
        case Severity.Warning => "  ⚠️"
      }
      s"$prefix ${err.message}"
    }.mkString("\n")
    s"$slideHeader\n$slideErrors"
  }.mkString("\n\n")

  header + formatted
}
```

Example output:
```
Validation failed with 3 error(s):

Slide 1:
  ❌ Missing required slot 'title'
  ⚠️ Body exceeds 12 lines (actual: 15, limit: 12)

Slide 3:
  ❌ Invalid image path: images/missing.png
```

### Parallel Validation with `parTraverse`

```scala
import cats.effect.IO
import cats.syntax.all._

// Validate slides in parallel (for large decks)
def validateDeckParallel(deck: SlideDeck): IO[ValidationResult[SlideDeck]] = {
  deck.slides
    .parTraverse { slide =>
      IO(validateSlide(slide)) // Each slide validation runs in parallel
    }
    .map { results =>
      val allErrors = results.flatten
      NonEmptyList.fromList(allErrors) match {
        case Some(errors) => Left(errors)
        case None         => Right(deck)
      }
    }
}
```

### Testing Pattern

```scala
class ValidationSpec extends munit.FunSuite {
  test("multiple errors collected") {
    val slide1 = Slide(id = 1, slots = Map.empty) // Missing title
    val slide2 = Slide(id = 2, slots = Map("title" -> "OK", "body" -> longBody)) // Density warning
    val deck = SlideDeck(List(slide1, slide2))

    val result = validateDeck(deck)

    assert(result.isLeft)
    val errors = result.left.get
    assertEquals(errors.size, 2)
    assert(errors.toList.exists(_.slideId == 1))
    assert(errors.toList.exists(_.slideId == 2))
  }

  test("success when no errors") {
    val validSlide = Slide(id = 1, slots = Map("title" -> "Title", "body" -> "Body"))
    val deck = SlideDeck(List(validSlide))

    val result = validateDeck(deck)

    assert(result.isRight)
  }
}
```

### Integration with Validation Pipeline (ADR-002)

```scala
// Combine Structure + Content errors
def validateComplete(deck: SlideDeck, theme: Theme): ValidationResult[SlideDeck] = {
  for {
    structureValid <- validateStructure(deck)  // Either[NonEmptyList[StructureError], SlideDeck]
    contentValid   <- validateContent(structureValid) // Either[NonEmptyList[ContentError], SlideDeck]
  } yield contentValid
  // If either fails, Left propagates; if both succeed, Right returned
  // Errors from first failure stop the chain (short-circuit)
}

// To accumulate errors from BOTH validators (instead of short-circuit):
import cats.data.Validated
import cats.syntax.all._

def validateCompleteAccumulating(deck: SlideDeck): ValidatedNel[ValidationError, SlideDeck] = {
  (
    validateStructure(deck).toValidated,
    validateContent(deck).toValidated
  ).mapN { (_, _) => deck }
  // Both validators run, errors accumulated even if first fails
}
```

---

**ADR Type**: Architectural Pattern
**Impact**: Domain layer (all validation code)
**Reversibility**: Medium (affects all validator signatures)
**Validation**: Validated in ceremony US-015
**Key Decision Rationale**: `NonEmptyList` provides type-safe error collection with better UX than fail-fast
