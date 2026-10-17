# PDR-005: Accessibility Requirements (WCAG AA)

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: US-009 (Theme Validation)

---

## Context

Accessible presentations are critical for:
- **Corporate compliance**: Many organizations require WCAG AA compliance
- **Legal requirements**: ADA (US), EAA (EU) mandate accessibility
- **Inclusive design**: Visually impaired users, color-blind users, low-vision users
- **Professional standards**: Conferences often require accessible slides

**Business Question**: What accessibility standards should MDSlides enforce?

**User Problems**:
1. **Low contrast**: Light gray text on white background (unreadable)
2. **Tiny fonts**: 12px font size on projected slides (illegible)
3. **Color-only meaning**: "Red means bad, green means good" (color-blind users miss meaning)
4. **Missing alt text**: Images without descriptions (screen readers fail)

**Standards Available**:
- **WCAG 2.1 Level A**: Minimum accessibility (4.5:1 contrast for normal text)
- **WCAG 2.1 Level AA**: Industry standard (recommended)
- **WCAG 2.1 Level AAA**: Gold standard (7:1 contrast - very strict)

---

## Decision

**MDSlides v1.0 Target**: **WCAG 2.1 Level AA**

### Required (Enforced in v1.0)

#### 1. Minimum Font Sizes
- **Title**: 36px minimum (48px recommended)
- **Body text**: 18px minimum (24px recommended)
- **Code**: 16px minimum (18px recommended)

**Rationale**: WCAG doesn't specify font sizes, but presentation context requires larger fonts (projected, viewed from distance)

**Enforcement**: Theme validation ERROR (blocks theme with fonts below minimums)

#### 2. Color Contrast Ratio
- **Normal text** (18px+): 4.5:1 minimum contrast ratio
- **Large text** (24px+): 3:1 minimum contrast ratio
- **Example**:
  - ✅ #333333 (dark gray) on #FFFFFF (white) = 12.6:1 (excellent)
  - ⚠️ #777777 (medium gray) on #FFFFFF (white) = 4.5:1 (minimum)
  - ❌ #AAAAAA (light gray) on #FFFFFF (white) = 2.3:1 (fails)

**Rationale**: WCAG AA requirement (Success Criterion 1.4.3)

**Enforcement**: Theme validation WARNING (non-blocking, but alerts theme author)

### Recommended (Not Enforced in v1.0)

#### 3. Alt Text for Images
- All images must have `alt` attribute
- Alt text describes image content (not "image" or "picture")

**Rationale**: WCAG AA requirement (Success Criterion 1.1.1)

**Enforcement v1.0**: None (image slides deferred to v1.1)
**Enforcement v1.1**: Content validation ERROR if image lacks alt text

#### 4. Keyboard Navigation
- Arrow keys (→, ←) navigate slides
- Space bar advances slide
- Home/End jump to first/last slide
- No mouse required

**Rationale**: WCAG AA requirement (Success Criterion 2.1.1 - Keyboard)

**Enforcement v1.0**: ✅ Implemented in HTML rendering (ADR-006)

#### 5. Semantic HTML
- Use proper heading hierarchy (h1, h2, not div with font styling)
- Use `<ul>` for lists, not ASCII bullets
- Use `<strong>` for emphasis, not just bold styling

**Rationale**: WCAG AA requirement (Success Criterion 1.3.1 - Info and Relationships)

**Enforcement v1.0**: ✅ Implemented via Scalatags (type-safe HTML)

---

## Consequences

### Positive

1. **Corporate Adoption**: Enterprises require WCAG compliance (competitive advantage)
2. **Inclusive Design**: Accessible to visually impaired, color-blind users
3. **Legal Compliance**: Meets ADA, EAA requirements
4. **Professional Quality**: High-contrast, readable slides are professional standard
5. **Conference Ready**: Many conferences require WCAG AA compliance

### Negative

1. **Design Constraints**: Low-contrast themes blocked (e.g., pastel colors)
2. **Font Size Minimums**: Small fonts disallowed (limits design freedom)
3. **Validation Overhead**: Contrast calculations add complexity

### Mitigations

1. **Clear Warnings**: Explain WHY contrast is too low, suggest alternatives
2. **Design Guidance**: Provide accessible color palettes in documentation
3. **Override Option**: v1.1+ could allow `--skip-accessibility-checks` for power users

---

## Alternatives Considered

### Alternative A: WCAG Level A (Minimum)
**Standards**: 3:1 contrast ratio, no font size minimums
**Why Rejected**:
- Too permissive (many unreadable slides would pass)
- Level AA is industry standard (A is outdated)
- Corporate compliance requires AA, not A

### Alternative B: WCAG Level AAA (Strictest)
**Standards**: 7:1 contrast ratio for normal text
**Why Rejected**:
- Too strict (blocks many valid color schemes)
- AAA is gold standard, not industry requirement
- Limits design flexibility unnecessarily

### Alternative C: No Accessibility Requirements
**Rationale**: Let authors decide (no validation)
**Why Rejected**:
- Corporate users can't adopt MDSlides (compliance blocker)
- Defeats purpose of opinionated tool (no authoring rails)
- Legal risk for enterprises

### Alternative D: Accessibility Optional (Opt-In)
**Approach**: `--check-accessibility` flag enables validation
**Why Rejected**:
- Most authors won't opt-in (default matters)
- Corporate users need it enforced by default
- Better to have accessible defaults

---

## Implementation Notes

### Color Contrast Calculation

```scala
def calculateContrast(foreground: String, background: String): Double = {
  val fgLuminance = relativeLuminance(parseHex(foreground))
  val bgLuminance = relativeLuminance(parseHex(background))

  val lighter = Math.max(fgLuminance, bgLuminance)
  val darker = Math.min(fgLuminance, bgLuminance)

  (lighter + 0.05) / (darker + 0.05)
}

def relativeLuminance(rgb: (Int, Int, Int)): Double = {
  val (r, g, b) = rgb
  val rs = r / 255.0
  val gs = g / 255.0
  val bs = b / 255.0

  val rLin = if (rs <= 0.03928) rs / 12.92 else Math.pow((rs + 0.055) / 1.055, 2.4)
  val gLin = if (gs <= 0.03928) gs / 12.92 else Math.pow((gs + 0.055) / 1.055, 2.4)
  val bLin = if (bs <= 0.03928) bs / 12.92 else Math.pow((bs + 0.055) / 1.055, 2.4)

  0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
}

def parseHex(hex: String): (Int, Int, Int) = {
  val clean = hex.stripPrefix("#")
  val r = Integer.parseInt(clean.substring(0, 2), 16)
  val g = Integer.parseInt(clean.substring(2, 4), 16)
  val b = Integer.parseInt(clean.substring(4, 6), 16)
  (r, g, b)
}
```

### Theme Validation

```scala
def validateTheme(theme: Theme): Either[ThemeValidationError, Theme] = {
  val errors = List.newBuilder[String]
  val warnings = List.newBuilder[String]

  // ERRORS (blocking)

  // 1. Font size minimums
  val bodySize = parsePx(theme.typography.bodySize)
  if (bodySize < 18) {
    errors += s"Body font size too small: ${bodySize}px (minimum 18px for accessibility)"
  }

  val titleSize = parsePx(theme.typography.titleSize)
  if (titleSize < 36) {
    errors += s"Title font size too small: ${titleSize}px (minimum 36px for accessibility)"
  }

  // WARNINGS (non-blocking)

  // 2. Contrast ratio
  val textContrast = calculateContrast(theme.colors.text, theme.colors.background)
  if (textContrast < 4.5) {
    warnings += s"Low contrast ratio for body text: ${textContrast}:1 (WCAG AA requires 4.5:1)"
    warnings += s"  → Text: ${theme.colors.text}, Background: ${theme.colors.background}"
    warnings += s"  → Suggestion: Use darker text (#333333) or lighter background (#FFFFFF)"
  }

  val headingContrast = calculateContrast(theme.colors.heading, theme.colors.background)
  if (headingContrast < 3.0) {
    warnings += s"Low contrast ratio for headings: ${headingContrast}:1 (WCAG AA requires 3:1 for large text)"
  }

  // Print warnings to stderr (non-blocking)
  if (warnings.nonEmpty) {
    System.err.println("⚠️  Accessibility Warnings:")
    warnings.result().foreach(w => System.err.println(s"  $w"))
  }

  // Return errors (blocking)
  errors.result() match {
    case Nil => Right(theme)
    case errs => Left(ThemeValidationError(errs))
  }
}
```

### Accessible Color Palettes (Documentation)

**High Contrast (WCAG AAA - 7:1)**:
```json
{
  "colors": {
    "background": "#FFFFFF",
    "text": "#000000",        // 21:1 contrast (maximum)
    "heading": "#1A1A1A",    // 18:1 contrast
    "accent": "#0051A5"      // 7.5:1 contrast
  }
}
```

**Dark Mode (WCAG AA - 4.5:1)**:
```json
{
  "colors": {
    "background": "#1E1E1E",
    "text": "#E0E0E0",        // 12:1 contrast
    "heading": "#FFFFFF",    // 15:1 contrast
    "accent": "#4FC3F7"      // 6:1 contrast
  }
}
```

**Corporate Blue (WCAG AA - 4.5:1)**:
```json
{
  "colors": {
    "background": "#F5F9FF",
    "text": "#2C3E50",        // 10:1 contrast
    "heading": "#003D7A",    // 8:1 contrast
    "accent": "#0066CC"      // 5:1 contrast
  }
}
```

---

## User Experience

### Success Path (Accessible Theme)

```bash
# Load theme with good contrast
mdslides slides.md --theme default
✓ Theme validation passed (contrast: 12.6:1, font sizes: 24px body / 48px title)
✓ Generated slides.html
```

### Failure Path (Font Too Small)

```bash
# Load theme with 12px body font
mdslides slides.md --theme tiny
❌ Theme validation failed

Theme 'tiny':
  ❌ Body font size too small: 12px (minimum 18px for accessibility)
     → Increase bodySize in theme to at least 18px
     → Recommended: 24px for optimal readability

No HTML generated.
```

### Warning Path (Low Contrast)

```bash
# Load theme with low contrast (non-blocking)
mdslides slides.md --theme pastel
⚠️  Accessibility Warnings:
  ⚠️  Low contrast ratio for body text: 3.2:1 (WCAG AA requires 4.5:1)
      → Text: #999999, Background: #F0F0F0
      → Suggestion: Use darker text (#666666) or white background (#FFFFFF)

✓ Generated slides.html (warnings present but non-blocking)
```

---

## WCAG 2.1 AA Compliance Checklist

### v1.0 Implemented ✅
- ✅ **1.4.3 Contrast (Minimum)**: Text contrast ≥ 4.5:1
- ✅ **2.1.1 Keyboard**: Full keyboard navigation (arrow keys, space, home/end)
- ✅ **1.3.1 Info and Relationships**: Semantic HTML (headings, lists)
- ✅ **2.4.6 Headings and Labels**: Proper heading hierarchy

### v1.1 Planned
- ⏭️ **1.1.1 Non-text Content**: Alt text required for images
- ⏭️ **2.4.4 Link Purpose**: Link text describes destination

### v1.2+ Future
- ⏭️ **1.4.4 Resize Text**: Text resizable to 200% without loss
- ⏭️ **1.4.12 Text Spacing**: Adjustable line height, spacing
- ⏭️ **2.5.5 Target Size**: Touch targets ≥ 44×44 CSS pixels

---

## Related Ceremonies

- **US-009**: Theme Validation (Three Amigos + Example Mapping - accessibility discussed)
- **Domain Modeling**: Theme aggregate, ColorScheme value object

---

## Related Governance

- **ADR-005**: Theme Integration Architecture (theme validation includes accessibility)
- **ADR-006**: Rendering Architecture (semantic HTML, keyboard navigation)
- **POL-001**: Ubiquitous Language (use "Accessibility" not "A11y" in documentation)

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: High (corporate compliance requirement)
**User Impact**: High (affects all theme authors, slide viewers)
**Legal Impact**: High (WCAG compliance reduces legal risk)
**Reversibility**: Low (relaxing accessibility standards is bad optics, bad practice)
