# PDR-001: Density Validation Limits

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: US-012 (Density Validation)

---

## Context

Slide presentations fail when slides are too dense - too much text, too many lines, text too small to read. This is a **pervasive problem** in corporate presentations:

- **Bullet-point overload**: 20+ lines of text on a single slide
- **Wall of text**: 300+ word paragraphs
- **Tiny fonts**: Text shrunk to fit content instead of removing content

**User Problem**: Authors create unreadable slides because they don't know what "fits on slide" means. No tool provides guidance.

**Business Question**: What limits should MDSlides enforce to ensure readable slides?

**Constraints**:
- Limits must work for 1920x1080 (16:9) slides (most common)
- Different themes might have different limits (corporate vs. minimal)
- Limits should be **guidance** (warnings), not hard blockers (errors)

---

## Decision

**Default density limits** (theme-configurable):
```yaml
maxBodyLines: 12        # Maximum lines of text in body slot
maxBodyWords: 150       # Maximum words in body slot
maxHeadingChars: 80     # Maximum characters in heading slot
```

**Validation Behavior**:
- **Warnings** (non-blocking): Exceeding limits generates warnings, HTML still generated
- **Theme Override**: Themes can define stricter limits (e.g., corporate theme: 8 lines max)
- **No Limits on Title Slide**: Title/subtitle/author slots exempt (different design)

**Rationale for Specific Limits**:

1. **12 Lines Maximum**:
   - 1080px height ÷ 60px padding ÷ 24px body font = ~40 lines total available
   - Subtract heading (2 lines) + spacing (10 lines) = ~28 lines usable
   - **50% rule**: Use only half available space for readability
   - **Result**: 12 lines fits comfortably with 24px font

2. **150 Words Maximum**:
   - 12 lines × ~12 words/line = 144 words
   - Round to 150 for flexibility
   - **Heuristic**: If you need more, split into 2 slides

3. **80 Characters Heading**:
   - 1920px width ÷ 120px padding ÷ 48px title font = ~37 chars/line
   - 2 lines × 37 chars = 74 chars
   - Round to 80 for flexibility
   - **Heuristic**: Headings should fit on 1-2 lines

---

## Consequences

### Positive

1. **Authoring Rails**: Authors get immediate feedback ("This slide is too dense")
2. **Key Differentiator**: Marp doesn't validate density - this is MDSlides' unique value
3. **Corporate Adoption**: Enterprises can enforce brand guidelines via custom themes
4. **Flexible**: Warnings (not errors) allow expert authors to override when necessary
5. **Theme Integration**: Different presentation contexts can have different standards

### Negative

1. **Learning Curve**: Authors must learn to write concise slides
2. **Pushback**: Some authors will want to disable warnings ("My slides are fine!")
3. **False Positives**: Some legitimately dense slides (e.g., legal disclaimers) will trigger warnings

### Mitigations

1. **Clear Messaging**: Warning text explains WHY slide is too dense
2. **Override Path**: Expert authors can ignore warnings (HTML still generated)
3. **Documentation**: Explain heuristics in user guide (not arbitrary limits)
4. **Theme Customization**: Authors can create permissive themes if needed

---

## Alternatives Considered

### Alternative A: No Density Validation
**Rationale**: Let authors decide what's readable
**Why Rejected**:
- Defeats MDSlides' value proposition (opinionated, guided authoring)
- Authors will create unreadable slides (proven problem)
- No competitive advantage over Marp

### Alternative B: Hard Errors (Blocking)
**Limits**: Same as above, but validation fails (no HTML generated)
**Why Rejected**:
- Too strict (frustrates authors)
- Some dense slides are legitimate (legal, references)
- Warnings provide guidance without blocking workflow

### Alternative C: Stricter Limits
**Limits**: 8 lines, 100 words, 60 chars
**Why Rejected**:
- Too strict for general use (though valid for corporate themes)
- Better to default to moderate limits, allow themes to be stricter

### Alternative D: Looser Limits
**Limits**: 20 lines, 300 words, 120 chars
**Why Rejected**:
- Too permissive (doesn't prevent dense slides)
- Loses value proposition (guidance disappears)

### Alternative E: AI-Based Readability
**Approach**: Use NLP to calculate readability scores (Flesch-Kincaid, etc.)
**Why Rejected**:
- Over-engineering for v1.0
- Simple line/word/char counts sufficient
- Could be v2.0 feature (ADR for AI integration)

---

## Implementation Notes

### Theme Schema
```json
{
  "name": "default",
  "layout": {
    "slideWidth": "1920px",
    "slideHeight": "1080px",
    "padding": "60px",
    "maxBodyLines": 12,      // Density limit
    "maxBodyWords": 150,     // Density limit
    "maxHeadingChars": 80    // Density limit
  }
}
```

### Corporate Theme Example
```json
{
  "name": "corporate-strict",
  "extends": "default",
  "layout": {
    "maxBodyLines": 8,       // Stricter
    "maxBodyWords": 100,     // Stricter
    "maxHeadingChars": 60    // Stricter
  }
}
```

### Validation Logic
```scala
def validateDensity(slide: Slide, theme: Theme): List[DensityWarning] = {
  val warnings = List.newBuilder[DensityWarning]

  slide.template match {
    case "content" =>
      slide.getSlot("body").foreach { body =>
        // Line count
        val lines = body.split("\n").length
        if (lines > theme.layout.maxBodyLines) {
          warnings += DensityWarning(
            slideId = slide.id,
            message = s"Body exceeds recommended line limit (guidance: reduce to ${theme.layout.maxBodyLines} lines for readability)",
            actual = lines,
            limit = theme.layout.maxBodyLines
          )
        }

        // Word count
        val words = body.split("\\s+").length
        if (words > theme.layout.maxBodyWords) {
          warnings += DensityWarning(
            slideId = slide.id,
            message = s"Body exceeds recommended word limit (guidance: reduce to ${theme.layout.maxBodyWords} words for readability)",
            actual = words,
            limit = theme.layout.maxBodyWords
          )
        }
      }

      slide.getSlot("heading").foreach { heading =>
        // Character count
        if (heading.length > theme.layout.maxHeadingChars) {
          warnings += DensityWarning(
            slideId = slide.id,
            message = s"Heading exceeds recommended character limit (guidance: reduce to ${theme.layout.maxHeadingChars} chars for readability)",
            actual = heading.length,
            limit = theme.layout.maxHeadingChars
          )
        }
      }

    case "title" =>
      // No density limits for title slides (different design)
      Nil
  }

  warnings.result()
}
```

### Warning Output Format
```
⚠️  Density Warnings (3):

Slide 5 (content):
  ⚠️  Body exceeds recommended line limit (actual: 15, guidance: 12 lines)
      → Consider splitting into 2 slides for better readability
  ⚠️  Body exceeds recommended word limit (actual: 180, guidance: 150 words)
      → Reduce content or split into multiple slides

Slide 8 (content):
  ⚠️  Heading exceeds recommended character limit (actual: 95, guidance: 80 chars)
      → Shorten heading or use subtitle

HTML generated successfully despite warnings.
Run with --validate-only to check without generating output.
```

---

## User Experience Considerations

### Success Path
1. Author writes slide with 10 lines, 120 words, 60-char heading
2. Validation passes (no warnings)
3. HTML generated successfully

### Warning Path
1. Author writes slide with 15 lines, 200 words, 90-char heading
2. Validation generates 3 density warnings (printed to stderr)
3. HTML still generated (warnings non-blocking)
4. Author can:
   - **Option A**: Revise slide to reduce density (recommended)
   - **Option B**: Ignore warnings (accept dense slide)
   - **Option C**: Create custom theme with higher limits

### Corporate Enforcement Path
1. Company creates `corporate-strict` theme (8 lines, 100 words, 60 chars)
2. Authors must use corporate theme (`mdslides slides.md --theme corporate-strict`)
3. Higher density triggers more warnings (enforces brand guidelines)
4. Corporate communications team reviews presentations for compliance

---

## Marketing / Positioning

**Value Proposition**: "MDSlides prevents unreadable slides by validating density"

**Competitive Advantage vs. Marp**:
| Feature | MDSlides | Marp |
|---------|----------|------|
| Density Validation | ✅ Yes (12 lines, 150 words) | ❌ No |
| Authoring Guidance | ✅ Warnings with suggestions | ❌ None |
| Corporate Themes | ✅ Theme-configurable limits | ❌ No validation |
| Opinionated | ✅ Guides authors to readable slides | ❌ Anything goes |

**User Testimonial Target**:
> "MDSlides saved my presentation. I didn't realize my slides were too dense until it warned me. Now my talks are much more engaging." — Marketing Director

---

## Future Enhancements (Not v1.0)

**v1.1**: Accessibility Warnings
- Color contrast ratio checks (WCAG AA)
- Font size minimums
- Alt text for images

**v1.2**: AI Readability Scoring
- Flesch-Kincaid readability grade level
- Suggest sentence simplifications
- Detect passive voice overuse

**v2.0**: Live Editing with Real-Time Warnings
- VS Code extension with live density feedback
- Red/yellow/green indicators in editor margin

---

## Related Ceremonies

- **US-012**: Density Validation (Three Amigos + Example Mapping)
- **US-008**: Theme Loading (theme limits integration)
- **Event Storming**: "DensityExceeded" domain event identified

---

## Related Governance

- **ADR-002**: Validation Pipeline Architecture (density as Phase 2a, warnings not errors)
- **ADR-005**: Theme Integration Architecture (theme loads before validation)
- **POL-001**: Ubiquitous Language (use "Density" not "Length" or "Size")

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: High (key differentiator)
**User Impact**: High (guides authors to better slides)
**Revenue Impact**: Medium (enterprise sales - theme customization)
**Reversibility**: Medium (limits can change, but breaking change for existing decks)
