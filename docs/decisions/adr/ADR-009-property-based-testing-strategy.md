# ADR-009: Property-Based Testing Strategy

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: Phase 3.3 (Property-Based Test Design Session)

---

## Context

Domain-Driven Design emphasizes **invariants** - business rules that must always hold. Example-based tests validate specific scenarios, but can miss edge cases:

```scala
// Example-based test (specific case)
test("title slot exceeding 80 chars triggers ContentError") {
  val slide = Slide(slots = Map("title" -> "x" * 81))
  assert(validate(slide).isLeft)
}
```

**Problem**: What about 79 chars? 80 chars exactly? Empty string? Null? Unicode characters?

**Property-based testing** (PBT) validates invariants across **thousands of generated inputs**:

```scala
// Property-based test (invariant)
property("validated slide title never exceeds 80 chars") {
  forAll(slideGen) { slide =>
    validate(slide) match {
      case Right(validSlide) =>
        validSlide.getSlot("title").forall(_.length <= 80)
      case Left(_) => true  // Validation rejected it (correct)
    }
  }
}
```

**Benefits**:
- Finds edge cases automatically (ScalaCheck generates thousands of test cases)
- Shrinks failures to minimal example (e.g., finds smallest failing input)
- Documents invariants as executable code

**Questions**:
1. Which PBT library to use? (ScalaCheck, Hedgehog, QuickCheck)
2. What to test with properties? (not everything needs PBT)
3. How to generate valid domain data? (not just random primitives)
4. How to integrate with existing test suite? (MUnit + ScalaCheck)

---

## Decision

**Use ScalaCheck** for property-based testing with the following strategy:

### 1. Library Choice: ScalaCheck

```scala
// build.sc
def ivyDeps = Agg(
  ivy"org.scalacheck::scalacheck:1.17.0",
  ivy"org.scalameta::munit-scalacheck:0.7.29"
)
```

**Rationale**:
- **ScalaCheck**: Standard PBT library for Scala (most mature, best ecosystem)
- **MUnit integration**: `munit-scalacheck` provides seamless integration
- **Shrinking**: ScalaCheck's shrinking is excellent (finds minimal failing examples)

### 2. Test Scope: Domain Invariants Only

**Test with properties**:
- ✅ Domain invariants (from domain model documentation)
- ✅ Round-trip properties (serialization, encoding)
- ✅ Idempotence properties (operations that don't change on repeat)

**Don't test with properties**:
- ❌ Infrastructure layer (I/O, parsing - too complex for PBT)
- ❌ Specific business scenarios (use example-based tests)
- ❌ Framework behavior (testing ScalaCheck, Flexmark, etc.)

### 3. Generator Strategy: Domain-Driven Generators

**Principle**: Generate valid domain data, not random primitives.

```scala
// ❌ Bad: Random primitives
val slideGen: Gen[Slide] = for {
  id <- Arbitrary.arbitrary[Int]  // Could be negative, zero, MAX_INT
  template <- Arbitrary.arbitrary[String]  // Could be "asdf", "", null
  slots <- Arbitrary.arbitrary[Map[String, String]]  // Random junk
} yield Slide(id, template, slots)

// ✅ Good: Valid domain data
val slideGen: Gen[Slide] = for {
  id <- Gen.choose(1, 200)  // Valid range (per PDR-003)
  template <- Gen.oneOf("title", "content")  // Known templates
  slots <- templateSlotGen(template)  // Slots match template
} yield Slide(id, template, slots)
```

### 4. Integration: Separate Test Files

**Structure**:
```
src/test/scala/com/tjmsolutions/mdslides/domain/
├── SlideSpec.scala                  // Example-based tests (MUnit)
├── SlideDeckSpec.scala              // Example-based tests
├── properties/
│   ├── SlideProperties.scala        // Property-based tests (ScalaCheck)
│   ├── SlideDeckProperties.scala
│   └── ThemeProperties.scala
└── generators/
    └── DomainGenerators.scala       // Shared generators
```

**Rationale**: Separation clarifies intent (examples vs. properties)

---

## Consequences

### Positive

1. **Edge Case Discovery**: ScalaCheck finds bugs we didn't anticipate
2. **Regression Prevention**: Invariants enforced automatically (CI runs properties)
3. **Living Documentation**: Properties document invariants as executable code
4. **Refactoring Confidence**: Change implementation, properties still pass (or catch breakage)
5. **Shrinking**: Minimal failing example makes debugging easier

### Negative

1. **Learning Curve**: Team must learn ScalaCheck, generator composition
2. **Slower Tests**: 100 test cases per property (vs. 1 example-based test)
3. **Generator Complexity**: Domain generators require careful design

### Risks

1. **Risk**: Poorly designed generators produce invalid data (false positives)
   - **Mitigation**: Code review generators, test generators themselves
2. **Risk**: Properties test implementation, not invariants
   - **Mitigation**: POL-004 policy, code review enforcement
3. **Risk**: Flaky properties (non-deterministic)
   - **Mitigation**: Ban time-based, I/O-based properties

---

## Alternatives Considered

### Alternative A: Hedgehog (Haskell-Inspired)

```scala
libraryDependencies += "qa.hedgehog" %% "hedgehog-sbt" % "0.10.1" % Test
```

**Why Rejected**:
- Smaller ecosystem (fewer tutorials, less community support)
- Integrated shrinking (good) but less flexible than ScalaCheck
- MUnit integration less mature

### Alternative B: QuickCheck (Haskell Original)

Not applicable (Haskell library, no Scala port besides ScalaCheck)

### Alternative C: No Property-Based Testing

**Rationale**: Example-based tests sufficient
**Why Rejected**:
- Misses edge cases (proven problem in industry)
- Doesn't leverage DDD invariants (wasted domain modeling effort)
- HOW-WE-WORK.md mandates PBT (Phase 3.3 ceremony)

### Alternative D: Property Tests Inline with Examples

**Structure**: Mix properties and examples in same test file
**Why Rejected**:
- Confusing (unclear which tests are properties vs. examples)
- Generator pollution (generators mixed with test logic)
- Harder to run properties separately (CI optimization)

---

## Implementation Notes

### ScalaCheck Configuration

```scala
// build.sc
object domain extends ScalaModule {
  def scalaVersion = "3.3.1"

  def ivyDeps = Agg(
    ivy"org.scalacheck::scalacheck:1.17.0"
  )

  object test extends ScalaTests with TestModule.Munit {
    def ivyDeps = Agg(
      ivy"org.scalameta::munit:0.7.29",
      ivy"org.scalameta::munit-scalacheck:0.7.29"
    )

    // ScalaCheck configuration
    def testFramework = "munit.Framework"

    override def forkArgs = Seq(
      "-Dscalacheck.minSuccessfulTests=100",  // Default
      "-Dscalacheck.maxDiscardRatio=5",       // Default
      "-Dscalacheck.workers=4"                // Parallel execution
    )
  }
}
```

### MUnit Integration

```scala
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class SlideProperties extends ScalaCheckSuite {
  import DomainGenerators._

  property("validated slide always has required slots") {
    forAll(slideGen) { slide =>
      validate(slide) match {
        case Right(validSlide) =>
          validSlide.getSlot("title").isDefined
        case Left(_) =>
          true  // Validation rejected it (expected)
      }
    }
  }

  // Override ScalaCheck config per-property if needed
  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters
      .withMinSuccessfulTests(500)  // More test cases
      .withMaxDiscardRatio(10)
}
```

### Domain Generators (Comprehensive)

```scala
package com.tjmsolutions.mdslides.domain.generators

import org.scalacheck.Gen
import com.tjmsolutions.mdslides.domain._

object DomainGenerators {

  // ========================================================================
  // Primitive Generators
  // ========================================================================

  val slideIdGen: Gen[Int] = Gen.choose(1, 200)

  val templateNameGen: Gen[String] = Gen.oneOf("title", "content")

  // Text generators with varying lengths
  val emptyTextGen: Gen[String] = Gen.const("")

  val shortTextGen: Gen[String] =
    Gen.alphaNumStr.map(_.take(50)).filter(_.nonEmpty)

  val mediumTextGen: Gen[String] =
    Gen.alphaNumStr.map(_.take(100)).filter(_.nonEmpty)

  val longTextGen: Gen[String] =
    Gen.alphaNumStr.map(_.take(200)).filter(_.nonEmpty)

  // Text with edge cases (empty, boundary, normal)
  val textGen: Gen[String] = Gen.frequency(
    (5, emptyTextGen),           // 5% empty
    (10, shortTextGen),          // 10% short
    (70, mediumTextGen),         // 70% medium (normal)
    (15, longTextGen)            // 15% long
  )

  // ========================================================================
  // Slot Generators
  // ========================================================================

  // Title slide slots (title required, subtitle/author optional)
  val titleSlotsGen: Gen[Map[String, String]] = for {
    title <- mediumTextGen
    subtitle <- Gen.option(mediumTextGen)
    author <- Gen.option(shortTextGen)
  } yield Map(
    "title" -> title
  ) ++ subtitle.map("subtitle" -> _).toMap
    ++ author.map("author" -> _).toMap

  // Content slide slots (heading + body required)
  val contentSlotsGen: Gen[Map[String, String]] = for {
    heading <- shortTextGen
    body <- longTextGen
  } yield Map(
    "heading" -> heading,
    "body" -> body
  )

  // Slots generator based on template type
  def slotsForTemplate(template: String): Gen[Map[String, String]] = {
    template match {
      case "title" => titleSlotsGen
      case "content" => contentSlotsGen
      case _ => Gen.const(Map.empty)
    }
  }

  // ========================================================================
  // Slide Generators
  // ========================================================================

  // Valid slide (template + slots match)
  val validSlideGen: Gen[Slide] = for {
    id <- slideIdGen
    template <- templateNameGen
    slots <- slotsForTemplate(template)
  } yield Slide(id, template, slots)

  // Invalid slide (missing required slots)
  val invalidSlideGen: Gen[Slide] = for {
    id <- slideIdGen
    template <- templateNameGen
    // Generate slots for WRONG template (structural mismatch)
    wrongTemplate <- templateNameGen.filter(_ != template)
    slots <- slotsForTemplate(wrongTemplate)
  } yield Slide(id, template, slots)

  // Slide with density issues
  val denseSlideGen: Gen[Slide] = for {
    id <- slideIdGen
    heading <- Gen.alphaNumStr.map(_.take(100))  // Exceeds 80 chars
    body <- Gen.listOfN(20, shortTextGen).map(_.mkString("\n"))  // Exceeds 12 lines
  } yield Slide(id, "content", Map("heading" -> heading, "body" -> body))

  // Combined slide generator (mix of valid/invalid/dense)
  val slideGen: Gen[Slide] = Gen.frequency(
    (70, validSlideGen),    // 70% valid
    (20, invalidSlideGen),  // 20% invalid structure
    (10, denseSlideGen)     // 10% density issues
  )

  // ========================================================================
  // SlideDeck Generators
  // ========================================================================

  // Valid deck (1-200 slides)
  val validDeckGen: Gen[SlideDeck] = for {
    count <- Gen.choose(1, 200)
    slides <- Gen.listOfN(count, validSlideGen)
  } yield SlideDeck(
    slides = slides.zipWithIndex.map { case (slide, i) => slide.copy(id = i + 1) }
  )

  // Empty deck (0 slides - invalid)
  val emptyDeckGen: Gen[SlideDeck] = Gen.const(SlideDeck(Nil))

  // Oversized deck (>200 slides - invalid)
  val oversizedDeckGen: Gen[SlideDeck] = for {
    count <- Gen.choose(201, 300)
    slides <- Gen.listOfN(count, validSlideGen)
  } yield SlideDeck(
    slides = slides.zipWithIndex.map { case (slide, i) => slide.copy(id = i + 1) }
  )

  // Combined deck generator
  val deckGen: Gen[SlideDeck] = Gen.frequency(
    (80, validDeckGen),      // 80% valid
    (10, emptyDeckGen),      // 10% empty
    (10, oversizedDeckGen)   // 10% oversized
  )

  // ========================================================================
  // Theme Generators
  // ========================================================================

  // Valid hex color
  val hexColorGen: Gen[String] = for {
    r <- Gen.choose(0, 255)
    g <- Gen.choose(0, 255)
    b <- Gen.choose(0, 255)
  } yield f"#$r%02X$g%02X$b%02X"

  // Font size (px)
  val fontSizeGen: Gen[String] =
    Gen.choose(12, 72).map(size => s"${size}px")

  // Valid theme
  val validThemeGen: Gen[Theme] = for {
    name <- Gen.alphaNumStr.filter(_.nonEmpty)
    bgColor <- hexColorGen
    textColor <- hexColorGen
    accentColor <- hexColorGen
    bodySize <- Gen.choose(18, 36).map(s => s"${s}px")  // Valid range
    titleSize <- Gen.choose(36, 72).map(s => s"${s}px") // Valid range
  } yield Theme(
    name = name,
    colors = ColorScheme(
      background = bgColor,
      text = textColor,
      accent = accentColor,
      heading = textColor,
      code = "#F5F5F5"
    ),
    typography = Typography(
      titleFont = "Arial",
      bodyFont = "Georgia",
      codeFont = "Consolas",
      titleSize = titleSize,
      bodySize = bodySize,
      codeSize = "18px"
    ),
    layout = LayoutSettings(
      slideWidth = "1920px",
      slideHeight = "1080px",
      padding = "60px",
      maxBodyLines = 12,
      maxBodyWords = 150,
      maxHeadingChars = 80
    )
  )

  // Invalid theme (font sizes too small)
  val invalidThemeGen: Gen[Theme] = for {
    theme <- validThemeGen
    tinyBodySize <- Gen.choose(8, 17).map(s => s"${s}px")  // Below 18px minimum
  } yield theme.copy(
    typography = theme.typography.copy(bodySize = tinyBodySize)
  )

  // Combined theme generator
  val themeGen: Gen[Theme] = Gen.frequency(
    (80, validThemeGen),    // 80% valid
    (20, invalidThemeGen)   // 20% invalid
  )
}
```

### Example Property Tests

```scala
package com.tjmsolutions.mdslides.domain.properties

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll
import com.tjmsolutions.mdslides.domain._
import com.tjmsolutions.mdslides.domain.generators.DomainGenerators._

class SlideProperties extends ScalaCheckSuite {

  property("validated slide always has template binding") {
    forAll(slideGen) { slide =>
      validateStructure(slide) match {
        case Right(validSlide) =>
          validSlide.template.nonEmpty &&
          List("title", "content").contains(validSlide.template)
        case Left(_) =>
          true  // Validation rejected it (expected)
      }
    }
  }

  property("validated slide has all required slots") {
    forAll(validSlideGen) { slide =>
      validateStructure(slide) match {
        case Right(validSlide) =>
          validSlide.template match {
            case "title" =>
              validSlide.getSlot("title").exists(_.nonEmpty)
            case "content" =>
              validSlide.getSlot("heading").exists(_.nonEmpty) &&
              validSlide.getSlot("body").exists(_.nonEmpty)
          }
        case Left(_) =>
          true
      }
    }
  }

  property("heading never exceeds 80 chars after validation") {
    forAll(slideGen.filter(_.template == "content")) { slide =>
      validateContent(slide) match {
        case Right(validSlide) =>
          validSlide.getSlot("heading").forall(_.length <= 80)
        case Left(_) =>
          true  // Validation rejected it
      }
    }
  }
}

class SlideDeckProperties extends ScalaCheckSuite {

  property("validated deck has 1-200 slides") {
    forAll(deckGen) { deck =>
      validateStructure(deck) match {
        case Right(validDeck) =>
          validDeck.slides.length >= 1 &&
          validDeck.slides.length <= 200
        case Left(_) =>
          true
      }
    }
  }

  property("deck validation is idempotent") {
    forAll(validDeckGen) { deck =>
      val result1 = validateStructure(deck)
      val result2 = result1.flatMap(validateStructure)
      result1 == result2
    }
  }
}

class ThemeProperties extends ScalaCheckSuite {

  property("validated theme has minimum font sizes") {
    forAll(themeGen) { theme =>
      validateTheme(theme) match {
        case Right(validTheme) =>
          parsePx(validTheme.typography.bodySize) >= 18 &&
          parsePx(validTheme.typography.titleSize) >= 36
        case Left(_) =>
          true
      }
    }
  }

  property("theme JSON round-trip preserves data") {
    forAll(validThemeGen) { theme =>
      val json = theme.toJson
      val parsed = Theme.fromJson(json)
      parsed == Right(theme)
    }
  }
}
```

---

## Testing Strategy

### Coverage Targets

**Example-based tests**: 90% line coverage (specific scenarios)
**Property-based tests**: Invariant coverage (all documented invariants tested)

### CI Integration

```yaml
# .github/workflows/test.yml
- name: Run unit tests
  run: mill domain.test

- name: Run property-based tests
  run: mill domain.test.testOnly -- munit.*.Properties

- name: Generate coverage report
  run: mill domain.test.coverage
```

### Performance Tuning

**Default**: 100 test cases per property
```scala
property("invariant holds") {
  forAll(gen) { value => ??? }
}
// Runs 100 times
```

**More coverage** (critical invariants):
```scala
override def scalaCheckTestParameters =
  super.scalaCheckTestParameters.withMinSuccessfulTests(1000)

property("critical invariant") {
  forAll(gen) { value => ??? }
}
// Runs 1000 times
```

**Faster feedback** (CI optimization):
```scala
// Local: 100 test cases (thorough)
// CI: 50 test cases (faster)
val testCases = sys.env.get("CI").fold(100)(_ => 50)

override def scalaCheckTestParameters =
  super.scalaCheckTestParameters.withMinSuccessfulTests(testCases)
```

---

## Debugging Failed Properties

### Shrinking Example

**Initial failure**:
```
Property failed with input:
  slide = Slide(42, "content", Map("heading" -> "asdfasdfasdfasdf...", "body" -> "..."))
```

**After shrinking**:
```
Minimal failing example:
  slide = Slide(1, "content", Map("heading" -> "a" * 81, "body" -> ""))
```

**Insight**: Heading exactly 81 chars fails (boundary at 80)

### Manual Shrinking Investigation

```scala
property("heading constraint") {
  forAll(slideGen) { slide =>
    val result = validateContent(slide)

    // Debug output (only on failure)
    result match {
      case Left(errors) =>
        println(s"Failed with slide: $slide")
        println(s"Errors: $errors")
      case _ => ()
    }

    // Property assertion
    result.isRight || slide.getSlot("heading").exists(_.length > 80)
  }
}
```

---

## Future Enhancements (Not v1.0)

**v1.1**: State Machine Testing
```scala
// Test slide state transitions
property("slide lifecycle respects state machine") {
  forAll(slideCommandsGen) { commands =>
    val finalState = commands.foldLeft(Slide.empty)(applyCommand)
    validState(finalState)
  }
}
```

**v1.2**: Model-Based Testing
```scala
// Generate entire slide deck editing session
property("deck editing session maintains invariants") {
  forAll(deckEditSessionGen) { session =>
    session.events.foldLeft(SlideDeck.empty) { (deck, event) =>
      applyEvent(deck, event)
    }.isValid
  }
}
```

---

## Related Policies

- **POL-004**: Property-Based Testing Requirements (mandate for all aggregates)
- **POL-003**: Pure Functional Domain (pure functions required for PBT)

---

## Related Ceremonies

- **Phase 3.3**: Property-Based Test Design Session (HOW-WE-WORK.md)

---

**ADR Type**: Testing Strategy
**Impact**: Test code (all domain tests)
**Reversibility**: Medium (switching PBT libraries is painful but possible)
**Validation**: Validated in Phase 3.3 ceremony (property-based test design)
**Key Decision Rationale**: ScalaCheck is industry standard for Scala PBT, mature ecosystem, excellent shrinking
