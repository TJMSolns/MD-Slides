# POL-002: Banned Terms in Domain Layer

**Status**: Active
**Date**: 2024-12-20
**Owners**: Tony Moores (Architect, Product Owner)

---

## Policy Statement

The following terms are BANNED from the domain layer (all code in `domain/` module): **Manager**, **Service**, **Handler**, **Util**, **Helper**, **DTO**, **Data**, **Processor**, **Controller**.

Code using these terms in domain class/method names will be rejected in code review.

---

## Rationale

These terms are **code smells** that indicate anemic domain models - objects with data but no behavior. Domain-Driven Design (DDD) emphasizes rich domain models where behavior lives with the data.

**Why These Terms Are Harmful**:

1. **Manager/Service/Handler**: Suggests procedural code (function bags) instead of object-oriented domain models
   - Example: `SlideManager.processSlide(slide)` → anemic (slide has no behavior)
   - Better: `slide.validate()` → rich (slide knows how to validate itself)

2. **Util/Helper**: Indicates missing domain concepts
   - Example: `SlideUtil.formatTitle(title)` → what domain concept is this?
   - Better: `Title(raw).formatted` → Title is a Value Object

3. **DTO/Data**: Indicates data structures without behavior
   - Example: `SlideData` → just a bag of fields
   - Better: `Slide` → has validation, rendering behavior

4. **Processor/Controller**: Web/framework terms leaking into domain
   - Example: `SlideProcessor` → what does "process" mean?
   - Better: `SlideDeckValidator` → clear domain responsibility

**Problem This Policy Prevents**:
- **Anemic domain models**: All logic in "Manager" classes, domain objects are just structs
- **Poor encapsulation**: Business rules scattered in utility classes
- **Weak ubiquitous language**: Technical jargon instead of domain concepts

---

## Scope

**Applies to**:
- Domain layer only (`domain/` module)
- Class names, trait names, object names
- Method names that imply these roles

**Exceptions**:
- **Infrastructure layer**: `ThemeLoader`, `MarkdownParser` are acceptable (I/O adapters)
- **CLI layer**: `ArgumentParser`, `ErrorHandler` are acceptable (technical concerns)
- **Test code**: `TestHelper`, `TestUtil` acceptable in test utilities (not domain tests)

---

## Enforcement

### Code Review Checklist
Reviewers MUST verify:
- [ ] No domain class names contain banned terms
- [ ] No method names imply "manager" pattern (e.g., `manageSlides`)
- [ ] Domain behavior lives in domain objects (not utility classes)

### Example Review Comments

❌ **REJECT**:
```scala
// domain/src/.../SlideManager.scala
class SlideManager {
  def processSlide(slide: Slide): Either[Error, Slide] = {
    // Validation logic here
  }
}
```
**Review comment**: "SlideManager is an anemic model. Move `processSlide` logic into `SlideDeckValidator` or make it a method on `Slide` itself."

❌ **REJECT**:
```scala
// domain/src/.../SlideUtil.scala
object SlideUtil {
  def formatTitle(title: String): String = title.trim.take(80)
}
```
**Review comment**: "SlideUtil is a utility dumping ground. Create a `Title` value object with `formatted` method instead."

✅ **APPROVE**:
```scala
// domain/src/.../Slide.scala
case class Slide(id: Int, template: String, slots: Map[String, String]) {
  def validateStructure(template: Template): Either[StructureError, Slide] = {
    // Validation logic here (behavior with data)
  }
}
```

✅ **APPROVE** (infrastructure layer):
```scala
// infrastructure/src/.../ThemeLoader.scala
class ThemeLoader {  // "Loader" acceptable (I/O concern, not domain)
  def load(themeName: String): IO[Theme] = ???
}
```

---

## Banned Terms Detailed

### Manager ❌
**Why banned**: Implies God object managing other objects (anemic domain)
**Alternative**: Use domain-specific terms (Validator, Renderer, Parser)

```scala
// ❌ Bad
class SlideDeckManager {
  def createDeck(): SlideDeck = ???
  def validateDeck(deck: SlideDeck): Boolean = ???
  def renderDeck(deck: SlideDeck): Html = ???
}

// ✅ Good
class SlideDeckFactory {  // Specific role: creation
  def create(): SlideDeck = ???
}

class SlideDeckValidator {  // Specific role: validation
  def validateStructure(deck: SlideDeck): Either[Error, SlideDeck] = ???
}

class SlideDeckRenderer {  // Specific role: rendering
  def render(deck: SlideDeck, theme: Theme): Html = ???
}
```

### Service ❌
**Why banned**: Generic term, often hides anemic models
**Alternative**: Use domain-specific terms or push behavior into entities

```scala
// ❌ Bad
class SlideService {
  def processSlide(slide: Slide): Slide = ???
}

// ✅ Good
case class Slide(...) {
  def withValidation(template: Template): Either[Error, Slide] = ???
}
```

### Handler ❌
**Why banned**: Event-driven framework term, not domain concept
**Alternative**: Use domain-specific verbs (Validator, Transformer)

```scala
// ❌ Bad
class ValidationErrorHandler {
  def handle(error: ValidationError): Unit = ???
}

// ✅ Good
class ValidationErrorFormatter {  // Specific role: formatting
  def format(errors: NonEmptyList[ValidationError]): String = ???
}
```

### Util / Helper ❌
**Why banned**: Dumping ground for unrelated functions (poor cohesion)
**Alternative**: Create domain Value Objects or specific domain classes

```scala
// ❌ Bad
object StringUtil {
  def truncate(s: String, max: Int): String = s.take(max)
  def isBlank(s: String): Boolean = s.trim.isEmpty
}

// ✅ Good
case class SlotContent(value: String) {
  def truncated(maxChars: Int): SlotContent = SlotContent(value.take(maxChars))
  def isBlank: Boolean = value.trim.isEmpty
}
```

### DTO (Data Transfer Object) ❌
**Why banned**: Anemic data structure (no behavior)
**Alternative**: Use domain entities/value objects with behavior

```scala
// ❌ Bad
case class SlideDTO(id: Int, title: String, body: String)  // No behavior

// ✅ Good
case class Slide(id: Int, template: String, slots: Map[String, String]) {
  def getSlot(name: String): Option[String] = slots.get(name)  // Behavior
  def validate(template: Template): Either[Error, Slide] = ???  // Behavior
}
```

### Data ❌
**Why banned**: Suffix suggests no behavior (e.g., `SlideData`)
**Alternative**: Remove "Data" suffix, add behavior

```scala
// ❌ Bad
case class ThemeData(colors: ColorScheme, fonts: Typography)  // Just data

// ✅ Good
case class Theme(colors: ColorScheme, typography: Typography) {
  def validate: Either[ThemeError, Theme] = ???  // Behavior
  def generateCSS: String = ???  // Behavior
}
```

### Processor ❌
**Why banned**: Vague, procedural (what does "process" mean?)
**Alternative**: Use domain-specific verbs

```scala
// ❌ Bad
class MarkdownProcessor {
  def process(markdown: String): SlideDeck = ???
}

// ✅ Good
class MarkdownParser {  // Specific role: parsing
  def parse(markdown: String): SlideDeck = ???
}
```

### Controller ❌
**Why banned**: Web framework term (MVC pattern), not domain concept
**Alternative**: Controllers belong in web layer, not domain

```scala
// ❌ Bad (in domain layer)
class SlideController {
  def createSlide(): Slide = ???
}

// ✅ Good (controller in web layer if needed, not domain)
// domain/src/...
case class Slide(...) {
  // Domain behavior here
}
```

---

## What to Use Instead

### Rich Domain Objects
```scala
// Domain entities with behavior
case class Slide(id: Int, template: String, slots: Map[String, String]) {
  def validateStructure(template: Template): Either[StructureError, Slide]
  def validateContent(template: Template): Either[ContentError, Slide]
  def renderWith(theme: Theme): Html
}

// Value objects with behavior
case class SlideId(value: Int) {
  require(value > 0, "Slide ID must be positive")
  def next: SlideId = SlideId(value + 1)
}
```

### Domain-Specific Classes
```scala
// Clear, specific domain roles
class SlideDeckValidator
class SlideDeckRenderer
class MarkdownParser
class ThemeValidator
```

### Functional Patterns
```scala
// Pure functions (no "Manager" wrapper needed)
object SlideDeck {
  def fromMarkdown(markdown: String): Either[ParseError, SlideDeck] = ???
}

// Type aliases for clarity
type ValidationResult[A] = Either[NonEmptyList[ValidationError], A]
```

---

## Exceptions

### Exception 1: Infrastructure Layer
**Allowed**: `ThemeLoader`, `FileHandler`, `MarkdownParser`
**Rationale**: Infrastructure adapters interface with I/O, frameworks. Names reflect technical concerns, not domain.

```scala
// ✅ Allowed in infrastructure/
class ThemeLoader {
  def load(themeName: String): IO[Theme] = ???
}

class MarkdownParser {  // "Parser" is infrastructure concern (Flexmark adapter)
  def parse(markdown: String): SlideDeck = ???
}
```

### Exception 2: Test Utilities
**Allowed**: `TestHelper`, `SlideTestUtil`
**Rationale**: Test infrastructure, not domain code.

```scala
// ✅ Allowed in test/
object SlideTestUtil {
  def createValidSlide(id: Int): Slide = ???
  def createInvalidSlide(): Slide = ???
}
```

### Exception 3: Explicit ADR Override
If a banned term is necessary (rare), create an ADR documenting:
- Why the term is needed
- Why alternatives won't work
- Approval from architect

**Approval Process**: Architect must approve via ADR or explicit code review comment.

---

## Related Policies

- **POL-001**: Ubiquitous Language Enforcement (use domain terms)
- **POL-003**: Pure Functional Domain (no side effects)

---

## Related Artifacts

- "Domain-Driven Design" by Eric Evans (Blue Book) - Chapter on Anemic Domain Models
- `doc/domain-models/ubiquitous-language.md` - Preferred domain terms

---

**Policy Owner**: Tony Moores (Architect)
**Enforcement**: Code review (manual)
**Next Review**: 2025-03-20 (quarterly)
