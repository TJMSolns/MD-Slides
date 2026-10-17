# POL-004: Property-Based Testing Requirements

**Status**: Active
**Date**: 2024-12-20
**Owners**: Tony Moores (Architect, Product Owner)

---

## Policy Statement

All domain aggregates MUST have property-based tests (PBT) that validate domain invariants using ScalaCheck. Property-based tests are REQUIRED in addition to example-based unit tests, not as a replacement.

Code review MUST verify that:
1. Each aggregate has at least one property-based test
2. All critical domain invariants are tested as properties
3. Generators produce valid domain data (not just random primitives)

---

## Rationale

Example-based tests (traditional unit tests) validate specific scenarios:
```scala
test("title slot exceeding 80 chars triggers ContentError") {
  val slide = Slide(slots = Map("title" -> "x" * 81))
  assert(validate(slide).isLeft)
}
```

**Problem**: Example tests only check cases you thought of. They miss edge cases.

Property-based tests validate invariants across thousands of generated inputs:
```scala
property("title slot never exceeds 80 chars after validation") {
  forAll(slideGen) { slide =>
    validate(slide) match {
      case Right(validSlide) =>
        validSlide.getSlot("title").forall(_.length <= 80)
      case Left(_) =>
        true  // Validation rejected it (correct)
    }
  }
}
```

**Benefit**: ScalaCheck generates thousands of test cases, finds edge cases you didn't anticipate.

**DDD Alignment**: Domain invariants (from domain modeling) should be properties. If an invariant is important enough to document, it's important enough to test with PBT.

**Problem This Policy Prevents**:
- **Edge case bugs**: Overlooked scenarios in production (e.g., empty strings, negative numbers, boundary values)
- **Regression bugs**: Changes break invariants silently (property tests catch this)
- **Incomplete specifications**: Invariants documented but not enforced

---

## Scope

**Applies to**:
- All domain aggregates (Slide, SlideDeck, Template, Theme)
- Domain value objects with constraints (SlideId, SlotContent, ValidationError)
- Domain functions with invariants (validation, transformation)

**Does NOT apply to**:
- Infrastructure layer (I/O adapters, parsers - too complex for PBT)
- CLI layer (integration tests sufficient)
- Simple DTOs with no invariants

---

## Enforcement

### Code Review Checklist

Reviewers MUST verify:
- [ ] Each aggregate has property-based tests in `src/test/.../properties/`
- [ ] All invariants from domain model documented as properties
- [ ] Generators produce valid domain data (use `Gen.choose`, `Gen.oneOf`, not `Arbitrary`)
- [ ] Properties fail when invariants violated (validate property correctness)
- [ ] Shrinking enabled (ScalaCheck default - don't disable)

### Example Review Comments

❌ **REJECT**:
```scala
// No property-based tests for Slide aggregate
class SlideSpec extends munit.FunSuite {
  test("title required") {
    val slide = Slide(slots = Map.empty)
    assert(validate(slide).isLeft)
  }
}
```
**Review comment**: "Missing property-based tests. Add `SlideProperties.scala` with properties for all invariants (required slots, max lengths, etc.)."

✅ **APPROVE**:
```scala
// Example-based tests
class SlideSpec extends munit.FunSuite {
  test("title required") { ??? }
}

// Property-based tests (separate file)
class SlideProperties extends munit.ScalaCheckSuite {
  property("validated slide always has required slots") {
    forAll(slideGen) { slide =>
      validate(slide) match {
        case Right(validSlide) =>
          validSlide.getSlot("title").isDefined
        case Left(_) => true
      }
    }
  }
}
```

---

## What to Test with Properties

### Domain Invariants (MUST Test)

Invariants are rules that ALWAYS hold. From domain modeling:

**Slide Invariants**:
```scala
// Invariant: Validated slide always has template binding
property("validated slide has valid template") {
  forAll(slideGen) { slide =>
    validate(slide) match {
      case Right(validSlide) =>
        validSlide.template.nonEmpty
      case Left(_) => true
    }
  }
}

// Invariant: Required slots always present after validation
property("validated slide has all required slots") {
  forAll(slideGen, templateGen) { (slide, template) =>
    validate(slide, template) match {
      case Right(validSlide) =>
        template.slots.filter(_.required).forall { slotDef =>
          validSlide.getSlot(slotDef.name).exists(_.nonEmpty)
        }
      case Left(_) => true
    }
  }
}
```

**SlideDeck Invariants**:
```scala
// Invariant: Deck size always 1-200 slides after validation
property("validated deck has 1-200 slides") {
  forAll(deckGen) { deck =>
    validateStructure(deck) match {
      case Right(validDeck) =>
        validDeck.slides.length >= 1 && validDeck.slides.length <= 200
      case Left(_) => true
    }
  }
}
```

**Theme Invariants**:
```scala
// Invariant: Valid theme has font sizes >= minimums
property("validated theme has minimum font sizes") {
  forAll(themeGen) { theme =>
    validateTheme(theme) match {
      case Right(validTheme) =>
        parsePx(validTheme.typography.bodySize) >= 18 &&
        parsePx(validTheme.typography.titleSize) >= 36
      case Left(_) => true
    }
  }
}
```

### Round-Trip Properties (SHOULD Test)

Serialization round-trips:
```scala
property("theme JSON round-trip preserves data") {
  forAll(themeGen) { theme =>
    val json = theme.toJson
    val parsed = Theme.fromJson(json)
    parsed == Right(theme)
  }
}
```

### Idempotence Properties (SHOULD Test)

Operations that don't change on repeated application:
```scala
property("validation is idempotent") {
  forAll(slideGen) { slide =>
    val result1 = validate(slide)
    val result2 = result1.flatMap(validate)
    result1 == result2
  }
}
```

---

## What NOT to Test with Properties

### Implementation Details
❌ **Don't test**:
```scala
property("validation uses NonEmptyList internally") {
  // Testing implementation, not behavior
}
```

### Framework Behavior
❌ **Don't test**:
```scala
property("Scalatags generates valid HTML") {
  // Testing library, not our code
}
```

### Non-Deterministic Behavior
❌ **Don't test**:
```scala
property("rendering time is consistent") {
  // Performance is non-deterministic (hardware, load, etc.)
}
```

---

## ScalaCheck Generators

### Generator Design Principles

1. **Generate valid domain data** (not random primitives):
```scala
// ❌ Bad: Random strings
val slideGen: Gen[Slide] = for {
  id <- Arbitrary.arbitrary[Int]
  template <- Arbitrary.arbitrary[String]  // Could be "asdfasdf"
  slots <- Arbitrary.arbitrary[Map[String, String]]
} yield Slide(id, template, slots)

// ✅ Good: Valid domain data
val slideGen: Gen[Slide] = for {
  id <- Gen.choose(1, 200)
  template <- Gen.oneOf("title", "content")
  slots <- templateSlotGen(template)  // Slots match template
} yield Slide(id, template, slots)
```

2. **Include edge cases explicitly**:
```scala
val titleSlotGen: Gen[String] = Gen.frequency(
  (80, Gen.alphaNumStr.map(_.take(80))),     // 80% normal cases
  (10, Gen.const("")),                        // 10% empty (edge case)
  (10, Gen.alphaNumStr.map(_.take(150)))     // 10% too long (boundary)
)
```

3. **Compose generators** (don't duplicate):
```scala
val validSlideGen: Gen[Slide] = for {
  id <- Gen.choose(1, 200)
  template <- Gen.oneOf("title", "content")
  slots <- validSlotsFor(template)  // Reusable
} yield Slide(id, template, slots)

val invalidSlideGen: Gen[Slide] = for {
  id <- Gen.choose(1, 200)
  template <- Gen.oneOf("title", "content")
  slots <- invalidSlotsFor(template)  // Reusable
} yield Slide(id, template, slots)
```

### Example Generators

```scala
import org.scalacheck.Gen

object DomainGenerators {
  // Slide ID (1-200)
  val slideIdGen: Gen[Int] = Gen.choose(1, 200)

  // Template name
  val templateGen: Gen[String] = Gen.oneOf("title", "content")

  // Slot content (varying lengths)
  val shortTextGen: Gen[String] = Gen.alphaNumStr.map(_.take(50))
  val mediumTextGen: Gen[String] = Gen.alphaNumStr.map(_.take(100))
  val longTextGen: Gen[String] = Gen.alphaNumStr.map(_.take(200))

  // Title slide slots
  val titleSlotsGen: Gen[Map[String, String]] = for {
    title <- shortTextGen
    subtitle <- Gen.option(mediumTextGen)
    author <- Gen.option(shortTextGen)
  } yield Map(
    "title" -> title,
    "subtitle" -> subtitle.getOrElse(""),
    "author" -> author.getOrElse("")
  ).filter(_._2.nonEmpty)

  // Content slide slots
  val contentSlotsGen: Gen[Map[String, String]] = for {
    heading <- shortTextGen
    body <- longTextGen
  } yield Map(
    "heading" -> heading,
    "body" -> body
  )

  // Slide (valid template binding)
  val slideGen: Gen[Slide] = for {
    id <- slideIdGen
    template <- templateGen
    slots <- template match {
      case "title" => titleSlotsGen
      case "content" => contentSlotsGen
    }
  } yield Slide(id, template, slots)

  // SlideDeck (1-200 slides)
  val deckGen: Gen[SlideDeck] = for {
    count <- Gen.choose(1, 200)
    slides <- Gen.listOfN(count, slideGen)
  } yield SlideDeck(slides.zipWithIndex.map { case (s, i) => s.copy(id = i + 1) })

  // Theme (valid colors, fonts)
  val themeGen: Gen[Theme] = for {
    name <- Gen.alphaNumStr
    bodySize <- Gen.choose(18, 36).map(s => s"${s}px")
    titleSize <- Gen.choose(36, 72).map(s => s"${s}px")
  } yield Theme(
    name = name,
    colors = ColorScheme("#FFFFFF", "#333333", "#0066CC", "#000000", "#F5F5F5"),
    typography = Typography("Arial", "Georgia", "Consolas", titleSize, bodySize, "18px"),
    layout = LayoutSettings("1920px", "1080px", "60px", 12, 150, 80)
  )
}
```

---

## Testing Strategy

### Test Organization

```
src/test/scala/com/tjmsolutions/mdslides/domain/
├── SlideSpec.scala                  // Example-based tests
├── SlideDeckSpec.scala              // Example-based tests
├── properties/
│   ├── SlideProperties.scala        // Property-based tests
│   ├── SlideDeckProperties.scala    // Property-based tests
│   └── ThemeProperties.scala        // Property-based tests
└── generators/
    └── DomainGenerators.scala       // Shared generators
```

### Test Naming Convention

**Example-based tests**: Specific scenarios
```scala
test("title slot exceeding 80 chars triggers ContentError")
test("missing required slot triggers StructureError")
```

**Property-based tests**: Invariants
```scala
property("validated slide always has required slots")
property("deck size always 1-200 after validation")
property("theme font sizes always >= minimums")
```

### Test Suite Size

**Example-based tests**: 5-10 tests per aggregate (key scenarios)
**Property-based tests**: 3-5 properties per aggregate (invariants)
**ScalaCheck runs**: 100 test cases per property (default - can increase)

---

## Common Pitfalls

### Pitfall 1: Testing Examples, Not Properties

❌ **Bad** (just a single example):
```scala
property("title required") {
  val slide = Slide(1, "title", Map.empty)
  validate(slide).isLeft
}
```

✅ **Good** (tests invariant):
```scala
property("validated slide always has title if template is 'title'") {
  forAll(slideGen.filter(_.template == "title")) { slide =>
    validate(slide) match {
      case Right(validSlide) => validSlide.getSlot("title").isDefined
      case Left(_) => true
    }
  }
}
```

### Pitfall 2: Unconstrained Generators

❌ **Bad** (generates invalid data):
```scala
val slideGen: Gen[Slide] = for {
  id <- Arbitrary.arbitrary[Int]  // Could be negative!
  template <- Arbitrary.arbitrary[String]  // Could be "xyz"!
} yield Slide(id, template, Map.empty)
```

✅ **Good** (generates valid domain data):
```scala
val slideGen: Gen[Slide] = for {
  id <- Gen.choose(1, 200)  // Valid range
  template <- Gen.oneOf("title", "content")  // Known templates
} yield Slide(id, template, Map.empty)
```

### Pitfall 3: Ignoring Shrinking

❌ **Bad** (disable shrinking):
```scala
property("invariant holds").withNoShrink {  // Don't do this!
  forAll(slideGen) { slide => ??? }
}
```

✅ **Good** (let ScalaCheck shrink):
```scala
property("invariant holds") {  // Shrinking enabled by default
  forAll(slideGen) { slide => ??? }
}
```

**Why?**: Shrinking finds minimal failing example (e.g., "title" → "" instead of "asdfasdfasdf" → "")

### Pitfall 4: Flaky Properties

❌ **Bad** (non-deterministic):
```scala
property("rendering is fast") {
  forAll(slideGen) { slide =>
    val start = System.currentTimeMillis()
    render(slide)
    val duration = System.currentTimeMillis() - start
    duration < 100  // Flaky! Depends on hardware, load
  }
}
```

✅ **Good** (deterministic):
```scala
property("rendered HTML contains slide content") {
  forAll(slideGen) { slide =>
    val html = render(slide)
    slide.getSlot("title").forall(title => html.contains(title))
  }
}
```

---

## Integration with Ceremonies

### Phase 3.3: Property-Based Test Design Session

**When**: After aggregate has passing example-based tests

**How**:
1. **Review domain model**: Identify invariants from `doc/domain-models/aggregates/`
2. **Write properties**: One property per invariant
3. **Write generators**: Valid + invalid data generators
4. **Run tests**: `sbt test` (ScalaCheck finds edge cases)
5. **Analyze failures**: Shrinking reveals minimal failing case
6. **Add to example map**: Add edge cases discovered to `doc/scenarios/example-maps/`
7. **Add BDD scenarios**: If edge case is user-facing, add Gherkin scenario

**Artifacts Created**:
- Property-based tests in `src/test/.../properties/`
- Generators in `src/test/.../generators/`
- Updated example maps with new edge cases

---

## Tools & Libraries

### Required
- **ScalaCheck**: Property-based testing for Scala
  ```scala
  libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
  ```

### Integration with MUnit
```scala
libraryDependencies += "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test

class SlideProperties extends munit.ScalaCheckSuite {
  property("invariant holds") {
    forAll(slideGen) { slide =>
      // Test here
    }
  }
}
```

### Configuration
```scala
// build.sc or build.sbt
override def testFramework = "munit.Framework"

// ScalaCheck configuration
Test / testOptions += Tests.Argument(
  TestFrameworks.ScalaCheck,
  "-minSuccessfulTests", "100",  // Default
  "-maxDiscardRatio", "5",       // Default
  "-workers", "4"                // Parallel execution
)
```

---

## Exceptions

### Exception 1: Infrastructure Layer
**Allowed**: No property-based tests for I/O adapters
**Rationale**: I/O is side-effectful, hard to test with PBT
**Alternative**: Integration tests, contract tests

### Exception 2: Trivial Aggregates
**Allowed**: No PBT if aggregate has no invariants
**Example**: Simple DTOs, wrappers
**Rationale**: No invariants = no properties to test

**Approval Process**: Architect must approve exception via code review comment.

---

## Related Policies

- **POL-003**: Pure Functional Domain (pure functions testable with PBT)
- **POL-001**: Ubiquitous Language (use domain terms in property names)

---

## Related Ceremonies

- **Phase 3.3**: Property-Based Test Design Session (HOW-WE-WORK.md lines 458-482)
- **Phase 1.3**: Domain Modeling Workshop (invariants identified)

---

## Related Governance

- **ADR-009**: Property-Based Testing Strategy (implementation details)

---

**Policy Owner**: Tony Moores (Architect)
**Enforcement**: Code review (manual)
**Next Review**: 2025-03-20 (quarterly)
