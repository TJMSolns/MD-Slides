# PDR-006: Code Block Rendering Limits

**Status:** Accepted
**Date:** 2024-12-21
**Deciders:** Product Team, Development Team
**Related:** US-004, PDR-001 (Density Limits), ADR-011 (Syntax Highlighting)

## Context

Code blocks in presentations pose unique challenges:
- Too many lines overwhelm audiences
- Code must be readable from a distance
- Technical presentations need real-world code examples
- Some code examples require more than 12 lines to be meaningful

### User Requirements

From Event Storming (December 21, 2024):
- **20-line guideline is reasonable** for code blocks
- **Auto-scale font size** to fit more lines on a slide if needed
- Code blocks should not fail validation outright (warn instead)

### Current Constraints (v0.1.0)

From PDR-001:
- Body text: max 12 lines, max 150 words
- Code blocks treated as plain text (counted toward 12-line limit)

### Problem

Meaningful code examples often exceed 12 lines:
- Function with error handling: 15-20 lines
- Class definition: 20-30 lines
- Test case with setup: 15-25 lines

Enforcing 12-line limit on code blocks forces presenters to:
- Omit important error handling
- Use incomplete examples
- Split examples awkwardly across slides

## Decision

### Code Block Line Limit

**Guideline**: 20 lines per code block (soft limit)
**Validation**: Warn (not fail) when code blocks exceed 20 lines
**Rendering**: Auto-scale font size to fit code on slide

### Rationale

1. **20 lines is readable**: Cognitive research shows 20-25 lines fit in working memory
2. **Flexibility**: Warnings allow presenters to exceed limit when justified
3. **Auto-scaling**: Font scaling ensures code fits on slide even if > 20 lines
4. **Separate from body**: Code blocks don't count toward body's 12-line limit

### Validation Rules

#### Code Block Validation (New)

```scala
case class CodeBlock(code: String, language: Option[String]):
  def lineCount: Int = code.split("\n").length

  def validate: Either[ValidationError, CodeBlock] =
    if lineCount > 20 then
      Left(DensityWarning(s"Code block exceeds 20 lines (has $lineCount). Consider splitting."))
    else
      Right(this)
```

#### Body Validation (Updated)

```scala
// Body line count excludes code blocks
def bodyLineCount(content: FormattedContent): Int =
  content.textLines.length  // Excludes code blocks and images
```

### Font Auto-Scaling

**Base font size**: 16px
**Minimum font size**: 10px (readability floor)
**Scaling formula**: `fontSize = max(10px, 16px * (20 / lineCount))`

**Examples:**
- 20 lines: 16px (no scaling)
- 30 lines: 10.7px → 10px (floor)
- 40 lines: 8px → 10px (floor)

**Implementation** (from ADR-011):

```javascript
function autoScaleCodeBlock(el) {
  const maxLines = 20;
  const lineCount = el.textContent.split('\n').length;

  if (lineCount > maxLines) {
    const scaleFactor = maxLines / lineCount;
    const baseFontSize = 16; // px
    const newFontSize = Math.max(10, baseFontSize * scaleFactor);
    el.style.fontSize = newFontSize + 'px';
  }
}
```

## Consequences

### Positive

- **Flexibility**: Presenters can include complete, realistic code examples
- **Readability**: 20-line guideline keeps most code readable
- **Warnings not errors**: Presenters make final decision on code length
- **Auto-scaling**: Long code blocks still fit on slide
- **Separation of concerns**: Code blocks have different constraints than body text

### Negative

- **Risk of abuse**: Presenters might include 50-line code blocks
- **Font scaling limits**: Below 10px, code becomes unreadable
- **Validation warnings**: Presenters might ignore warnings

### Mitigation

- **Clear documentation**: Explain why 20 lines is recommended
- **Validation messages**: Provide actionable feedback ("Consider splitting into 2 slides")
- **Example presentations**: Show best practices with code blocks
- **Font floor**: 10px minimum prevents completely unreadable code

## Validation Error Messages

### Warning (not blocking)

```
⚠ Slide 3: Code block exceeds 20-line guideline (has 32 lines).
  Consider splitting into multiple slides for better readability.
```

### Informational

```
ℹ Slide 3: Code block (32 lines) will render with scaled font (11px).
  Audience may struggle to read code from a distance.
```

## Examples

### Example 1: Reasonable Code Block (15 lines)

```scala
def parseSlide(markdown: String): Either[ParseError, Slide] =
  for {
    frontmatter <- extractFrontmatter(markdown)
    templateName <- frontmatter.get("template").toRight(ParseError("Missing template"))
    content <- extractContent(markdown)
    slots <- parseSlots(templateName, content)
    id <- generateSlideId()
  } yield Slide(id, templateName, slots)
```

**Validation**: ✓ Pass (15 < 20)
**Rendering**: 16px font (no scaling)

### Example 2: Long Code Block (28 lines)

```scala
test("validate content slide with all constraints") {
  val heading = "Test Heading"
  val body = """
    Line 1
    Line 2
    ...
    Line 12
  """.trim

  val slide = Slide(
    SlideId.unsafe(1),
    "content",
    Map(
      "heading" -> heading,
      "body" -> body
    )
  )

  val result = Slide.validated(slide.id, slide.templateName, slide.slots)

  result match
    case Right(validSlide) =>
      assertEquals(validSlide.getSlot("heading"), Some(heading))
      assertEquals(validSlide.getSlot("body"), Some(body))
    case Left(errors) =>
      fail(s"Expected success, got: ${errors}")
}
```

**Validation**: ⚠ Warning (28 > 20)
**Rendering**: 11.4px font (scaled)

### Example 3: Excessive Code Block (50 lines)

```
(50 lines of code)
```

**Validation**: ⚠ Warning (50 > 20)
**Rendering**: 10px font (floor reached)
**Recommendation**: Split into 2-3 slides

## Related Constraints

### Body Text (unchanged from PDR-001)

- Max 12 lines (excluding code blocks, images)
- Max 150 words (excluding code blocks, image alt text)

### Heading (unchanged from PDR-001)

- Max 80 characters
- Single line

### Complete Validation Example

```markdown
---
template: content
---
## Code Example Validation (78 chars, OK)

This code demonstrates validation.
Word count excludes code below.

```scala
def validate(x: Int): Either[Error, Int] =
  if x > 0 then Right(x)
  else Left(Error("Must be positive"))
```

Back to body text after code block.
```

**Validation:**
- Heading: 78 chars ✓
- Body (text only): 8 lines ✓, ~14 words ✓
- Code block: 3 lines ✓ (no warning)
- **Result**: ✓ Pass

## Decision Drivers

1. **Cognitive Load**: 20 lines balances completeness with readability
2. **Real-World Code**: Meaningful examples often need 15-20 lines
3. **Presenter Autonomy**: Warnings respect presenter judgment
4. **Technical Feasibility**: Auto-scaling ensures code fits on slide
5. **User Feedback**: User confirmed "20 lines is reasonable"

## References

- [PDR-001: Density Validation Limits](PDR-001-density-limits.md)
- [ADR-011: Syntax Highlighting Approach](../ADR/ADR-011-syntax-highlighting-approach.md)
- [US-004: Code Block Support](../CEREMONIES-v0.2.0.md#us-004-code-block-support)
- Cognitive Load Theory (Miller's Law: 7±2 items, extended to ~20 lines for code)

## Revision History

- **2024-12-21**: Initial version (v0.2.0)
