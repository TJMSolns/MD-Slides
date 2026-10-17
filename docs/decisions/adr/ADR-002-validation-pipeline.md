# ADR-002: Validation Pipeline Architecture

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-011 (Structure Validation), US-012 (Density Validation), US-013 (Content Validation), US-015 (Error Collection)

---

## Context

SlideDeck validation must enforce multiple types of constraints:
- **Structure**: Template binding, slot presence, type matching
- **Density**: "Fits on slide" heuristics (12 lines, 150 words, 80 chars)
- **Content**: Template-specific rules (e.g., title max 80 chars, no code blocks in content slides)

**Key Questions**:
1. What validation order? (sequential vs. parallel)
2. How to handle dependencies? (e.g., Content validation needs Structure to pass first)
3. Errors vs. warnings? (blocking vs. non-blocking)
4. Fail-fast vs. error collection?

**Constraints**:
- **User Experience**: Authors should see ALL errors at once (not one-at-a-time)
- **Performance**: Validation should be < 100ms for 50-slide deck
- **Maintainability**: Each validation type should be independently testable
- **Density as Differentiator**: Density validation is MDSlides' key feature vs. Marp

---

## Decision

**3-Tier Validation Pipeline** with dependency-aware execution:

```
┌─────────────────────────────────────────────────┐
│ Phase 1: STRUCTURE Validation (ERRORS)         │
│ - Template binding check                        │
│ - Required slot presence                        │
│ - Slot type matching                            │
└─────────────┬───────────────────────────────────┘
              │
              ├─ PASS ──────────┬─────────────────┐
              │                  │                  │
              │         ┌────────▼──────┐  ┌───────▼──────┐
              │         │ Phase 2a:     │  │ Phase 2b:    │
              │         │ DENSITY       │  │ CONTENT      │
              │         │ (WARNINGS)    │  │ (ERRORS)     │
              │         │               │  │              │
              │         │ Runs in       │  │ Blocked by   │
              │         │ parallel      │  │ Structure    │
              │         └───────────────┘  └──────────────┘
              │
              └─ FAIL ─────────────────────────────────────┐
                                                            │
                              ┌─────────────────────────────▼──┐
                              │ Skip Content Validation         │
                              │ (Structure must pass first)     │
                              └─────────────────────────────────┘
```

**Execution Rules**:
1. **Structure Validation** always runs first (Phase 1)
2. **Density Validation** runs in parallel with Content (Phase 2a) - independent
3. **Content Validation** only runs if Structure passed (Phase 2b) - dependent
4. All errors/warnings collected before returning

**Error Classification**:
- **ERRORS**: Blocking issues (Structure, Content) → exit 1, no HTML generated
- **WARNINGS**: Non-blocking issues (Density) → exit 0, HTML generated with warnings logged

---

## Consequences

### Positive

1. **Better UX**: Authors see all errors at once (not fail-fast)
2. **Clear Separation**: Each validation type independently testable
3. **Dependency Management**: Content validation doesn't run on malformed slides
4. **Performance**: Density + Content run in parallel when possible
5. **Differentiator**: Density warnings highlight MDSlides' unique value
6. **Flexible**: Easy to add new validation types (e.g., Accessibility in v1.1)

### Negative

1. **Complexity**: 3 validation types to maintain
2. **Dependency Logic**: Must ensure Content doesn't run if Structure failed
3. **Error Aggregation**: Need NonEmptyList to collect errors from multiple validators

### Risks

1. **Risk**: Validation performance degrades with large decks
   - **Mitigation**: Parallel execution (Cats Effect parMapN), target < 100ms for 50 slides
2. **Risk**: Unclear error messages when multiple validators fail
   - **Mitigation**: Group errors by slide, format clearly (see US-015)

---

## Alternatives Considered

### Alternative A: Single Monolithic Validator
**Structure**:
```scala
def validate(deck: SlideDeck): Either[NonEmptyList[ValidationError], SlideDeck] = {
  // All validation logic in one function
  for {
    _ <- checkStructure(deck)
    _ <- checkDensity(deck)
    _ <- checkContent(deck)
  } yield deck
}
```
**Why Rejected**:
- No separation of concerns
- Can't skip Content validation if Structure fails
- Hard to test individual validation types
- Can't run Density + Content in parallel

### Alternative B: Fail-Fast Pipeline
**Structure**:
```scala
deck
  .validateStructure()  // Fails immediately on first error
  .flatMap(_.validateDensity())
  .flatMap(_.validateContent())
```
**Why Rejected**:
- Poor UX (author fixes one error, runs again, sees next error)
- Doesn't leverage error collection pattern (ADR-003)
- Slower feedback loop

### Alternative C: All Validations in Parallel
**Structure**:
```scala
(
  validateStructure(deck),
  validateDensity(deck),
  validateContent(deck)
).parMapN { (struct, density, content) => deck }
```
**Why Rejected**:
- Content validation assumes Structure passed (e.g., slot access)
- Would get confusing errors (Content errors on malformed slides)
- Doesn't respect dependency order

### Alternative D: Treat All as Errors (No Warnings)
**Why Rejected**:
- Defeats purpose of Density validation (should be guidance, not blocker)
- Authoring rails too strict (expert authors should override density limits)
- Reduces flexibility (some decks legitimately need dense slides)

---

## Implementation Notes

### Type Signatures

```scala
// Phase 1: Structure Validation
def validateStructure(deck: SlideDeck): Either[NonEmptyList[StructureError], SlideDeck]

// Phase 2a: Density Validation (always runs)
def validateDensity(deck: SlideDeck, theme: Theme): List[DensityWarning]

// Phase 2b: Content Validation (only if Structure passed)
def validateContent(deck: SlideDeck): Either[NonEmptyList[ContentError], SlideDeck]

// Orchestrator
def validateSlideDeck(
  deck: SlideDeck,
  theme: Theme
): Either[NonEmptyList[ValidationError], (SlideDeck, List[DensityWarning])] = {
  validateStructure(deck) match {
    case Left(structErrors) =>
      // Structure failed → skip Content, but still run Density
      val densityWarnings = validateDensity(deck, theme)
      Left(structErrors.widen[ValidationError])

    case Right(validDeck) =>
      // Structure passed → run Content + Density in parallel
      val densityWarnings = validateDensity(validDeck, theme)
      validateContent(validDeck) match {
        case Left(contentErrors) =>
          Left(contentErrors.widen[ValidationError])
        case Right(validatedDeck) =>
          Right((validatedDeck, densityWarnings))
      }
  }
}
```

### Error ADT (from ADR-003)

```scala
sealed trait ValidationError {
  def slideId: SlideId
  def message: String
  def severity: Severity
}

enum Severity {
  case Error   // Blocking (Structure, Content)
  case Warning // Non-blocking (Density)
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

### Validation Order Enforcement

```scala
// Enforced by type system + orchestrator logic
// Content validator receives SlideDeck (not raw parsed data)
// Guarantees Structure validation passed
trait ContentValidator {
  def validate(deck: SlideDeck): Either[NonEmptyList[ContentError], SlideDeck]
  //             ^^^^^^^^^^^ Only accepts validated structure
}
```

### Performance Targets

- **Structure Validation**: < 50ms (simple checks, no theme needed)
- **Density Validation**: < 30ms (text counting, theme limits)
- **Content Validation**: < 20ms (template constraint checks)
- **Total Pipeline**: < 100ms for 50-slide deck

### Testing Strategy

Each validator independently testable:
```scala
class StructureValidatorSpec extends munit.FunSuite {
  test("missing required slot") {
    val slide = Slide(/* missing title slot */)
    val result = validateStructure(SlideDeck(List(slide)))
    assert(result.isLeft)
    assertEquals(result.left.get.head.message, "Required slot 'title' is missing")
  }
}
```

### Future Extensions

Easy to add new validation types:
```scala
// v1.1: Accessibility Validation
def validateAccessibility(deck: SlideDeck, theme: Theme): List[AccessibilityWarning]

// v1.2: SEO Validation
def validateSEO(deck: SlideDeck): List[SEOWarning]
```

---

**ADR Type**: Architectural Pattern
**Impact**: Domain layer, all validation code
**Reversibility**: Medium (refactoring possible but affects all validators)
**Validation**: Validated in ceremonies US-011, US-012, US-013, US-015
**Key Decision Rationale**: Dependency-aware execution + error collection = better UX + maintainability
