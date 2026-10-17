# POL-005: Code Documentation Standards

**Status**: Active
**Date**: 2024-12-20
**Owners**: Tony Moores (Architect, Product Owner)

---

## Policy Statement

Code MUST be self-documenting (clear names, simple logic). Comments and documentation are SUPPLEMENTS, not substitutes for clear code.

**Required Documentation**:
1. Public API methods MUST have Scaladoc
2. Complex algorithms MUST have "why" comments (not "what")
3. Each module MUST have README.md

**Forbidden Documentation**:
1. NO comments explaining obvious code
2. NO comments restating code
3. NO commented-out code in production

Code review MUST verify documentation standards are followed.

---

## Rationale

**Bad Documentation Problem**:
```scala
// ❌ Bad: Comment restates code
// Set the title to the value
slide.title = value
```

**Good Code Problem**:
```scala
// ✅ Good: Self-documenting
slide.setTitle(value)
```

**Why This Policy Exists**:

1. **Comments Lie**: Code changes, comments don't (stale comments are worse than no comments)
2. **Self-Documenting Code**: Good names eliminate most comment needs
3. **Signal-to-Noise**: Too many comments = important comments buried
4. **Maintenance Burden**: Comments require maintenance (another thing to update)

**DDD Alignment**: Ubiquitous language in code names eliminates need for translation comments.

**Problem This Policy Prevents**:
- **Comment clutter**: 50% of file is comments explaining trivial code
- **Stale comments**: "This returns a User" but code returns Optional[Customer]
- **Missing comments**: Complex algorithm has zero explanation (opposite extreme)

---

## Scope

**Applies to**:
- All Scala code (domain, infrastructure, CLI)
- Public APIs (exposed to other modules)
- Complex algorithms (non-obvious logic)

**Does NOT apply to**:
- Test code (tests are documentation - can have more comments)
- Generated code (e.g., Scalameta macros)
- External libraries (we don't control their documentation)

---

## Enforcement

### Code Review Checklist

Reviewers MUST verify:
- [ ] Public APIs have Scaladoc (purpose, parameters, return value, exceptions)
- [ ] No trivial comments (restating code)
- [ ] No commented-out code (use Git history instead)
- [ ] Complex logic has "why" comments (not "what")
- [ ] Module has README.md (if new module)
- [ ] Domain code uses ubiquitous language (reduces comment needs)

### Example Review Comments

❌ **REJECT: Trivial comments**:
```scala
// Get the slide ID
val id = slide.id

// Check if the title is empty
if (title.isEmpty) {
  // Return an error
  return Left(TitleEmptyError)
}
```
**Review comment**: "Remove trivial comments. Code is self-explanatory. If names are unclear, rename variables instead."

❌ **REJECT: Commented-out code**:
```scala
def validate(slide: Slide): Either[Error, Slide] = {
  // val oldValidation = slide.checkStructure()
  // if (oldValidation.failed) return Left(StructureError)

  validateStructure(slide)
}
```
**Review comment**: "Remove commented-out code. Use Git history if you need to see old implementation."

❌ **REJECT: Missing Scaladoc on public API**:
```scala
// Public API, no Scaladoc
def validateSlideDeck(deck: SlideDeck): Either[NonEmptyList[ValidationError], SlideDeck] = {
  ???
}
```
**Review comment**: "Add Scaladoc to public API. Document purpose, parameters, return value, and possible errors."

✅ **APPROVE: Clear code, minimal comments**:
```scala
/**
 * Validates slide deck structure, density, and content.
 *
 * @param deck The slide deck to validate
 * @return Right(validatedDeck) if all validations pass, Left(errors) otherwise
 *
 * Validation order: Structure → Density + Content (parallel)
 * See ADR-002 for validation pipeline architecture.
 */
def validateSlideDeck(deck: SlideDeck): Either[NonEmptyList[ValidationError], SlideDeck] = {
  for {
    structureValid <- validateStructure(deck)
    densityWarnings = validateDensity(deck, theme)  // Non-blocking
    contentValid <- validateContent(structureValid)
  } yield contentValid
}
```

✅ **APPROVE: Complex algorithm with "why" comment**:
```scala
def calculateContrast(fg: String, bg: String): Double = {
  // WCAG 2.1 contrast calculation uses relative luminance, not simple RGB difference
  // Formula: (lighter + 0.05) / (darker + 0.05)
  // See: https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio
  val fgLuminance = relativeLuminance(parseHex(fg))
  val bgLuminance = relativeLuminance(parseHex(bg))

  val lighter = Math.max(fgLuminance, bgLuminance)
  val darker = Math.min(fgLuminance, bgLuminance)

  (lighter + 0.05) / (darker + 0.05)
}
```

---

## Documentation Requirements by Code Type

### 1. Public APIs (MUST Have Scaladoc)

**What is a Public API?**
- Methods exposed to other modules
- Domain model constructors
- Validation functions
- Rendering functions

**Required Scaladoc Elements**:
```scala
/**
 * [One-line summary of what this does]
 *
 * [Optional: Longer description if needed]
 *
 * @param paramName [Parameter description]
 * @return [What is returned, including Right/Left cases for Either]
 * @throws ExceptionType [When this exception is thrown - if applicable]
 *
 * [Optional: Example usage]
 * {{{
 * val result = validateSlide(slide)
 * }}}
 *
 * [Optional: See also references]
 * @see [[RelatedClass]]
 * @see ADR-XXX for architecture decision
 */
def publicMethod(paramName: ParamType): ReturnType = ???
```

**Example**:
```scala
/**
 * Validates slide structure (template binding, required slots, type matching).
 *
 * Structure validation is the first phase of the validation pipeline.
 * If structure validation fails, content validation is skipped (dependency).
 *
 * @param slide The slide to validate
 * @param templates Available templates for binding validation
 * @return Right(slide) if structure is valid, Left(errors) if invalid
 *
 * Common errors:
 * - Missing required slots (e.g., title slot in title template)
 * - Unknown template name
 * - Slot type mismatch
 *
 * @see ADR-002 Validation Pipeline Architecture
 * @see POL-001 Ubiquitous Language Enforcement
 */
def validateStructure(
  slide: Slide,
  templates: Map[String, Template]
): Either[NonEmptyList[StructureError], Slide] = ???
```

### 2. Private/Internal Methods (NO Scaladoc Required)

**Private methods should be self-documenting**:
```scala
// ✅ Good: No Scaladoc needed (name is clear)
private def extractH1(nodes: List[Node]): String = {
  nodes.collectFirst { case h: Heading if h.getLevel == 1 => h.getText }
    .getOrElse("")
}

// ❌ Bad: Unnecessary Scaladoc
/**
 * Extracts the H1 heading from nodes.
 * @param nodes The nodes to search
 * @return The H1 text
 */
private def extractH1(nodes: List[Node]): String = ???
```

**Exception**: Private method with complex logic MAY have comment explaining "why"
```scala
// Calculate luminance using sRGB color space formula (not simple average)
// Gamma correction required for perceptual accuracy
private def relativeLuminance(rgb: (Int, Int, Int)): Double = {
  val (r, g, b) = rgb
  val rs = r / 255.0
  val gs = g / 255.0
  val bs = b / 255.0

  // Apply gamma correction (WCAG formula)
  val rLin = if (rs <= 0.03928) rs / 12.92 else Math.pow((rs + 0.055) / 1.055, 2.4)
  val gLin = if (gs <= 0.03928) gs / 12.92 else Math.pow((gs + 0.055) / 1.055, 2.4)
  val bLin = if (bs <= 0.03928) bs / 12.92 else Math.pow((bs + 0.055) / 1.055, 2.4)

  0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
}
```

### 3. Case Classes / ADTs (Scaladoc for Domain Concepts)

**Domain entities/value objects**:
```scala
/**
 * A slide in a slide deck.
 *
 * Slides bind to templates (e.g., "title", "content") and provide slot content.
 * Each slide has a unique ID within its deck (1-indexed).
 *
 * Invariants:
 * - ID must be positive (1-200)
 * - Template must be known template name
 * - Required slots (per template) must be non-empty
 *
 * @param id Slide number (1-indexed, unique within deck)
 * @param template Template name (e.g., "title", "content")
 * @param slots Map of slot name → content
 * @param notes Optional speaker notes (parsed but not rendered in v1.0)
 *
 * @see Template for slot definitions
 * @see ADR-008 Slot-Based Content Model
 */
case class Slide(
  id: Int,
  template: String,
  slots: Map[String, String],
  notes: Option[String] = None
)
```

**Simple value objects** (minimal Scaladoc):
```scala
/**
 * Unique identifier for a slide within a deck (1-200).
 */
case class SlideId(value: Int) {
  require(value > 0 && value <= 200, "Slide ID must be 1-200")
}
```

### 4. Complex Algorithms (Comment the "Why", Not the "What")

**Bad (comments restate code)**:
```scala
// ❌ Bad: "What" comments
def validateDensity(slide: Slide, theme: Theme): List[DensityWarning] = {
  // Get the body slot
  val body = slide.getSlot("body").getOrElse("")

  // Split by newlines
  val lines = body.split("\n")

  // Get the line count
  val lineCount = lines.length

  // Check if it exceeds the max
  if (lineCount > theme.layout.maxBodyLines) {
    // Return a warning
    List(DensityWarning(slide.id, "Too many lines", lineCount, theme.layout.maxBodyLines))
  } else {
    // Return empty list
    Nil
  }
}
```

**Good (comments explain "why")**:
```scala
// ✅ Good: "Why" comments
def validateDensity(slide: Slide, theme: Theme): List[DensityWarning] = {
  val body = slide.getSlot("body").getOrElse("")
  val lines = body.split("\n")

  // Density validation is non-blocking (warnings only)
  // Authors can exceed limits if they have good reason
  // See PDR-001 for density limit rationale (12 lines = 50% of available space)
  if (lines.length > theme.layout.maxBodyLines) {
    List(DensityWarning(
      slide.id,
      s"Body exceeds recommended line limit (guidance: reduce to ${theme.layout.maxBodyLines} lines for readability)",
      lines.length,
      theme.layout.maxBodyLines
    ))
  } else {
    Nil
  }
}
```

### 5. Module README.md (REQUIRED for Each Module)

**Every module MUST have README.md** documenting:
- **Purpose**: What this module does
- **Key Concepts**: Main abstractions (1-2 sentences each)
- **Dependencies**: What this module depends on
- **Public API**: Entry points for other modules
- **Examples**: Basic usage example

**Example: `domain/README.md`**:
```markdown
# Domain Module

## Purpose

Pure functional domain model for MDSlides. Contains all business logic (validation, transformations) with no side effects.

## Key Concepts

- **Slide**: Single presentation slide with template binding and slot content
- **SlideDeck**: Collection of slides (1-200) with metadata
- **Template**: Slot definitions and constraints for slide types
- **Theme**: Visual styling (colors, fonts, layout) with density limits

## Invariants

All invariants are tested with property-based tests (see `test/properties/`):
- Slide IDs are 1-200 (positive, within deck size limit)
- Required slots are non-empty after validation
- Validated decks have 1-200 slides
- Themes have minimum font sizes (18px body, 36px title)

## Public API

Main entry points from other modules:
- `validateSlideDeck`: Structure + Density + Content validation
- `renderSlideDeck`: Generate HTML from validated deck + theme

## Dependencies

- **Cats**: NonEmptyList for error collection, Either for validation results
- **No other dependencies** (pure domain, no I/O)

## Architecture

See ADR-007 (Pure Functional Domain Model) for design decisions.
```

**Example: `infrastructure/README.md`**:
```markdown
# Infrastructure Module

## Purpose

Adapters for external libraries (Flexmark, Scalatags, Circe). Bridges pure domain with I/O operations.

## Key Components

- **MarkdownParser**: Flexmark adapter (Markdown → SlideDeck AST)
- **ThemeLoader**: Circe adapter (JSON → Theme)
- **HtmlGenerator**: Scalatags adapter (SlideDeck → HTML)

## Dependencies

- Domain module (pure functions)
- Flexmark (Markdown parsing)
- Scalatags (HTML generation)
- Circe (JSON parsing)

## Architecture

See ADR-001 (Technology Stack) for library choices.
```

---

## What NOT to Document

### 1. Obvious Code (Self-Documenting)

❌ **Don't document**:
```scala
// Set the title
slide.title = "My Title"

// Loop through slides
slides.foreach { slide =>
  // Validate the slide
  validate(slide)
}
```

✅ **Just write clearly**:
```scala
slide.setTitle("My Title")

slides.foreach(validate)
```

### 2. Temporary Code (TODOs, FIXMEs)

❌ **Don't commit commented TODOs**:
```scala
// TODO: Add validation for images
// FIXME: This is broken
// HACK: Workaround for bug in Flexmark
```

✅ **Create GitHub issues instead**:
```scala
// Temporary workaround for Flexmark issue #1234
// Remove when upstream is fixed (tracked in issue #56)
def workaround(): Unit = ???
```

### 3. Code You Didn't Change

❌ **Don't add docs to existing code during unrelated changes**:
```scala
// PR: "Add density validation"
// Changed file: Slide.scala

// ❌ Bad: Added Scaladoc to unrelated method
/**
 * Returns the slide ID.
 * @return The ID
 */
def getId(): Int = id  // Didn't touch this method!
```

✅ **Only document what you changed**:
```scala
// PR: "Add density validation"

// ✅ Good: Only documented new method
/**
 * Validates slide density (line count, word count, char count).
 * Returns warnings (non-blocking) if limits exceeded.
 */
def validateDensity(theme: Theme): List[DensityWarning] = ???
```

### 4. Implementation Details Users Don't Need

❌ **Don't document internals in public API**:
```scala
/**
 * Validates slide deck.
 *
 * Implementation details:
 * - Uses NonEmptyList for error collection
 * - Calls validateStructure, then validateDensity in parallel with validateContent
 * - Structure errors stored in list buffer, then converted to NonEmptyList
 * - Uses for-comprehension for sequencing
 */
def validateSlideDeck(deck: SlideDeck): ValidationResult[SlideDeck] = ???
```

✅ **Document behavior, not implementation**:
```scala
/**
 * Validates slide deck structure, density, and content.
 *
 * Returns all validation errors (not fail-fast).
 * Density validation produces warnings (non-blocking).
 *
 * @param deck The slide deck to validate
 * @return Right(deck) if valid, Left(errors) if invalid
 *
 * @see ADR-002 Validation Pipeline Architecture
 */
def validateSlideDeck(deck: SlideDeck): ValidationResult[SlideDeck] = ???
```

---

## Scaladoc Generation

### Generate API Documentation

```bash
# Generate Scaladoc
mill domain.docJar

# Output location
ls out/domain/docJar.dest/javadoc/
```

### Scaladoc Configuration

```scala
// build.sc
object domain extends ScalaModule {
  def scalaVersion = "3.3.1"

  def scalacOptions = Seq(
    "-deprecation",
    "-feature",
    "-Xfatal-warnings",
    "-doc-title", "MDSlides Domain API",
    "-doc-version", "1.0.0"
  )
}
```

### Documentation Structure

```
out/domain/docJar.dest/javadoc/
├── index.html                          # API overview
├── com/
│   └── tjmsolutions/
│       └── mdslides/
│           └── domain/
│               ├── Slide.html           # Slide API docs
│               ├── SlideDeck.html       # SlideDeck API docs
│               ├── validation/
│               │   └── Validator.html
│               └── rendering/
│                   └── Renderer.html
```

---

## Testing Documentation

### Scaladoc Examples (Doctests)

**Not supported in Scala 3 yet** (unlike Rust, Python)

**Workaround**: Put examples in test code
```scala
// Slide.scala (production)
/**
 * Creates a slide with title template.
 *
 * Example usage:
 * {{{
 * val slide = Slide.title(id = 1, title = "My Presentation")
 * }}}
 */
def title(id: Int, title: String): Slide = ???

// SlideSpec.scala (test)
test("create title slide") {
  val slide = Slide.title(id = 1, title = "My Presentation")
  assertEquals(slide.template, "title")
  assertEquals(slide.getSlot("title"), Some("My Presentation"))
}
```

---

## Comment Style Guide

### Single-Line Comments

```scala
// Prefer "//" for single-line comments
val result = validate(slide)  // Inline comment if needed
```

### Multi-Line Comments

```scala
// Prefer multiple "//" for multi-line
// Each line starts with "//"
// Easier to extend, easier to read in diffs
```

**Avoid `/* */` except for Scaladoc**:
```scala
// ❌ Avoid
/*
 * Multi-line comment
 * using C-style syntax
 */

// ✅ Prefer
// Multi-line comment
// using consistent "//" style
```

### TODOs / FIXMEs (Temporary Only)

```scala
// Allowed during development (remove before merge):
// TODO: Add image validation (issue #123)
// FIXME: Handle edge case (slide with no slots)

// Use consistent format:
// TODO(username): Description (issue #N)
// FIXME(username): Description (issue #N)
```

**Code review MUST verify** all TODOs/FIXMEs resolved or converted to GitHub issues.

---

## Exceptions

### Exception 1: Generated Code
**Allowed**: Lack of documentation in generated code (e.g., Scalameta macros)
**Rationale**: Generated code changes frequently, docs become stale

### Exception 2: Test Code
**Allowed**: More liberal commenting in tests (explain test intent)
**Rationale**: Tests are documentation, comments help readability

### Exception 3: Exploratory Prototypes
**Allowed**: No documentation in spike branches
**Rationale**: Prototypes are throwaway (don't waste time documenting)

**Approval Process**: Architect must approve exceptions via code review comment.

---

## Related Policies

- **POL-001**: Ubiquitous Language Enforcement (good names reduce comment needs)
- **POL-002**: Banned Terms in Domain Layer (avoid generic names that need comments)
- **POL-003**: Pure Functional Domain (pure functions easier to document)

---

## Related Artifacts

- **ADR-007**: Pure Functional Domain Model (documents "why" pure, code shows "how")
- **Ceremony Documentation**: Living documentation (doc/) is primary source of truth

---

## Quick Reference

| Code Type | Documentation Required | Example |
|-----------|----------------------|---------|
| Public API | ✅ Scaladoc (purpose, params, return) | `def validateSlideDeck(...)` |
| Private method | ❌ No (unless complex algorithm) | `private def extractH1(...)` |
| Case class (domain) | ✅ Scaladoc (purpose, invariants) | `case class Slide(...)` |
| Case class (simple) | ⚠️ One-liner Scaladoc | `case class SlideId(value: Int)` |
| Complex algorithm | ✅ "Why" comments (not "what") | WCAG contrast calculation |
| Module | ✅ README.md (purpose, API, examples) | `domain/README.md` |
| Trivial code | ❌ No comments | `slide.title = value` |

---

**Policy Owner**: Tony Moores (Architect)
**Enforcement**: Code review (manual)
**Next Review**: 2025-03-20 (quarterly)
