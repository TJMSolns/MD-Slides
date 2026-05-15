# POL-001: Ubiquitous Language Enforcement

**Status**: Active
**Date**: 2024-12-20
**Owners**: Tony Moores (Architect, Product Owner)

---

## Policy Statement

All code (classes, methods, variables, packages) in the domain layer MUST use terms from the MDSlides Ubiquitous Language as defined in `doc/domain-models/ubiquitous-language.md`. Code that uses terms not in the ubiquitous language will be rejected in code review.

---

## Rationale

Domain-Driven Design (DDD) requires that the code reflect the domain model. The Ubiquitous Language bridges the gap between business experts and developers. When code uses domain terms consistently:
- **Developers** can understand business logic without translation
- **Business experts** can read code (variable/class names) and verify correctness
- **Onboarding** is faster (new team members learn domain from code)
- **Refactoring** is safer (domain concepts remain stable even as implementation changes)

**Problem This Policy Prevents**:
- Code drift: technical jargon replaces domain concepts (e.g., `SlideProcessor` instead of `SlideDeckValidator`)
- Confusion: same concept has multiple names (e.g., `Deck` vs. `Presentation` vs. `SlideSet`)
- Onboarding friction: new developers must learn both domain AND code vocabulary

---

## Scope

**Applies to**:
- Domain layer (all code in `domain/` module)
- Class names, method names, variable names, package names
- Public APIs exposed to other modules
- Test code that exercises domain logic

**Does NOT apply to**:
- Infrastructure layer implementation details (e.g., Flexmark internals)
- Third-party libraries (Cats, Scalatags, etc.)
- Temporary variables in small scopes (e.g., loop counters)
- CLI layer (command-line argument names follow POSIX conventions)

---

## Enforcement

### Automated Checks
**v1.0** (No automation yet):
- Manual code review only

**v1.1+** (Future):
- Custom Scalafix rule to check class/method names against ubiquitous language dictionary
- CI/CD pipeline blocks PRs with violations

### Code Review Checklist
Reviewers MUST verify:
- [ ] All domain classes use ubiquitous language terms (e.g., `SlideDeck`, not `Presentation`)
- [ ] All domain methods use domain verbs (e.g., `validateStructure`, not `checkSlide`)
- [ ] No banned terms used (see POL-002)
- [ ] New domain concepts added to `ubiquitous-language.md`

### Example Review Comments
❌ **Reject**:
```scala
class SlideProcessor {  // "Processor" not in ubiquitous language
  def checkSlide(slide: Slide): Boolean = ???  // "check" ambiguous
}
```
**Review comment**: "Please rename `SlideProcessor` to `SlideDeckValidator` (ubiquitous language term) and `checkSlide` to `validateStructure`."

✅ **Approve**:
```scala
class SlideDeckValidator {
  def validateStructure(slide: Slide): Either[StructureError, Slide] = ???
}
```

---

## Exceptions

### Exception 1: Scala Standard Library Conflicts
**Example**: `Template` (domain term) conflicts with Scala's `StringContext.s` template method
**Resolution**: Use fully qualified name or alias: `import com.tjmsolutions.mdslides.domain.{Template => SlideTemplate}`

### Exception 2: Well-Known Design Patterns
**Example**: `Factory`, `Builder`, `Repository`
**Resolution**: Allowed ONLY in infrastructure layer, not domain layer

**Approval Process**: Architect must approve exceptions via ADR or code review comment.

---

## Ubiquitous Language Reference

**Core Entities**:
- `SlideDeck` (not: Presentation, Deck, SlideSet)
- `Slide` (not: Page, Frame)
- `Template` (not: Layout, Format)
- `Theme` (not: Style, Skin)
- `Slot` (not: Field, Placeholder, Section)

**Validation Terms**:
- `ValidationError` (not: Error, Issue, Problem)
- `StructureError` (structural validation failures)
- `DensityWarning` (density heuristic violations - non-blocking)
- `ContentError` (content constraint violations)
- `validateStructure` (not: checkStructure, verifyStructure)
- `validateDensity` (not: checkLength, verifySize)

**Rendering Terms**:
- `renderSlideDeck` (not: generateHTML, buildOutput)
- `renderSlide` (not: convertSlide, makeSlide)

**Parsing Terms**:
- `parseMarkdown` (not: readMarkdown, loadMarkdown)
- `parseSlideDeck` (not: extractSlides, buildDeck)

**Full Reference**: See `doc/domain-models/ubiquitous-language.md`

---

## Testing Adherence

### Test Class Names
Tests MUST use ubiquitous language:
```scala
// ✅ Good
class SlideDeckValidatorSpec extends munit.FunSuite {
  test("validateStructure detects missing required slots") { ??? }
}

// ❌ Bad
class SlideCheckerTest extends munit.FunSuite {
  test("check returns false for invalid slides") { ??? }
}
```

### Test Scenario Names
Use domain terms in test descriptions:
```scala
// ✅ Good
test("title slot exceeding 80 chars triggers ContentError") { ??? }

// ❌ Bad
test("long title fails") { ??? }
```

---

## Onboarding Support

**New Developer Checklist**:
1. Read `doc/domain-models/ubiquitous-language.md` (required)
2. Review `doc/domain-models/event-storming.md` (recommended)
3. Read 2-3 ceremony documents (Three Amigos sessions) to see terms in context
4. Run `grep -r "class " domain/src/` and verify class names match ubiquitous language

**Pair Programming**:
- Encourage pairing for first 2-3 domain stories
- Senior developer points out ubiquitous language usage

---

## Maintenance

**Updating Ubiquitous Language**:
1. New domain concepts discovered during development
2. Architect updates `ubiquitous-language.md`
3. Git commit message: `"docs(ubiquitous-language): add XYZ term"`
4. Notify team via Slack/email

**Quarterly Review**:
- Architect reviews `ubiquitous-language.md` for completeness
- Check for code drift (terms in code not in dictionary)
- Update dictionary as needed

---

## Examples

### Example 1: Class Naming
```scala
// ❌ Violation
class SlideManager {  // "Manager" not in ubiquitous language (see POL-002)
  def process(slide: Slide): Unit = ???
}

// ✅ Correct
class SlideValidator {  // "Validator" is domain concept
  def validateStructure(slide: Slide): Either[StructureError, Slide] = ???
}
```

### Example 2: Method Naming
```scala
// ❌ Violation
def checkDeck(deck: SlideDeck): Boolean = ???  // "check" ambiguous

// ✅ Correct
def validateStructure(deck: SlideDeck): Either[StructureError, SlideDeck] = ???
```

### Example 3: Variable Naming
```scala
// ❌ Violation
val presentation = parseSlideDeck(markdown)  // "presentation" not in ubiquitous language

// ✅ Correct
val deck = parseSlideDeck(markdown)  // "deck" is short form of "SlideDeck" (acceptable)
```

### Example 4: Package Naming
```scala
// ❌ Violation
package com.tjmsolutions.mdslides.processing  // "processing" vague

// ✅ Correct
package com.tjmsolutions.mdslides.domain.validation
package com.tjmsolutions.mdslides.domain.rendering
```

---

## Related Policies

- **POL-002**: Banned Terms in Domain Layer (forbidden terms)
- **POL-003**: Pure Functional Domain (no side effects)

---

## Related Artifacts

- `doc/domain-models/ubiquitous-language.md` (authoritative source)
- `doc/domain-models/event-storming.md` (domain concepts in context)
- All Three Amigos sessions (usage examples)

---

**Policy Owner**: Tony Moores (Architect)
**Enforcement**: Code review (manual), future Scalafix rules (automated)
**Next Review**: 2025-03-20 (quarterly)
