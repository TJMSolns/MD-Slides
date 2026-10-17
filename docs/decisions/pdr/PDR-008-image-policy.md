# PDR-008: Image Policy (Background vs Content)

**Status:** Accepted
**Date:** 2024-12-21
**Deciders:** Product Team, Development Team
**Related:** US-005, PDR-001 (Density Limits), PDR-005 (Accessibility), PDR-007 (Theme Schema)

## Context

Images in presentations serve two distinct purposes:
1. **Content images**: Diagrams, charts, photos that convey information
2. **Background images**: Aesthetic/branding elements that don't convey primary information

These require different validation rules and density constraints.

### User Requirements

From Event Storming (December 21, 2024):
- **1 background image per slide** (or per template via theme)
- **Multiple content images allowed** (validated for visual density)
- Background images specified in theme or template frontmatter
- Content images embedded in slide body via markdown

### Current v0.1.0 State

- No image support
- All validation based on text density

## Decision

### Image Types

#### 1. Background Images

**Purpose**: Branding, aesthetics, watermarks
**Specification**: Theme JSON or template frontmatter
**Limit**: 1 per slide
**Validation**: Not validated for accessibility (decorative)

**Example (Theme):**
```json
{
  "background": {
    "image": "./themes/corporate/background.png",
    "opacity": 0.1
  }
}
```

**Example (Template Frontmatter):**
```markdown
---
template: content
backgroundImage: ./images/section-bg.png
backgroundOpacity: 0.15
---
## Slide Content
...
```

#### 2. Content Images

**Purpose**: Information delivery (diagrams, charts, photos)
**Specification**: Markdown in slide body
**Limit**: No hard limit (validated for visual density)
**Validation**: Alt text required (PDR-005 accessibility)

**Example:**
```markdown
---
template: content
---
## Architecture Overview

Our system has three layers:

![System Architecture Diagram](./diagrams/architecture.png)

Each layer has distinct responsibilities.
```

### Validation Rules

#### Background Image Validation

1. **Count**: Max 1 background image per slide
   - Template frontmatter `backgroundImage` OR
   - Theme `background.image`
   - If both specified, template frontmatter overrides theme

2. **File Existence**: NOT validated
   - Graceful fallback if image missing
   - Browser handles broken image links

3. **Accessibility**: NOT required
   - Background images are decorative
   - No alt text needed

**Validation Error (if multiple backgrounds):**
```
✗ Slide 1: Multiple background images specified (template frontmatter + theme).
  Template frontmatter will override theme background.
```

#### Content Image Validation

1. **Alt Text**: Required (PDR-005)
   - Markdown: `![alt text](url)`
   - Empty alt text not allowed

2. **Visual Density**: Soft limit, warning-based
   - 1-2 images: ✓ Pass
   - 3-4 images: ⚠ Warning ("High visual density")
   - 5+ images: ⚠ Warning ("Excessive visual density, consider splitting")

3. **Word/Line Count**: Images excluded
   - Images don't count toward body's 150-word limit
   - Images don't count toward body's 12-line limit
   - Only surrounding text counts

**Validation Errors:**

```
✗ Slide 2: Content image missing required alt text.
  Image: ./diagrams/flow.png
  Fix: ![Describe the diagram](./diagrams/flow.png)
```

```
⚠ Slide 3: High visual density (4 content images).
  Consider splitting content across multiple slides for clarity.
```

### Word/Line Counting with Images

**Example Slide:**
```markdown
---
template: content
---
## System Architecture

Our system has three layers shown below.

![Layer 1: Presentation](./img/layer1.png)
![Layer 2: Business Logic](./img/layer2.png)
![Layer 3: Data Access](./img/layer3.png)

Each layer is independently deployable.
```

**Word Count:**
- "Our system has three layers shown below." = 8 words
- "Each layer is independently deployable." = 5 words
- **Total**: 13 words (images excluded) ✓

**Line Count:**
- "Our system has three layers shown below." = 1 line
- "Each layer is independently deployable." = 1 line
- **Total**: 2 lines (images excluded) ✓

**Visual Density:**
- 3 content images ⚠ Warning

### Domain Model

#### Value Objects

```scala
// Background Image (template or theme level)
case class BackgroundImage(
  url: String,
  opacity: Double = 1.0,
  position: String = "center",
  size: String = "cover"
)

object BackgroundImage:
  def validated(url: String, opacity: Double): Either[ValidationError, BackgroundImage] =
    if opacity < 0.0 || opacity > 1.0 then
      Left(ContentError("Background image opacity must be between 0.0 and 1.0"))
    else
      Right(BackgroundImage(url, opacity))

// Content Image (in slide body)
case class ContentImage(
  url: String,
  altText: String
)

object ContentImage:
  def validated(url: String, altText: String): Either[ValidationError, ContentImage] =
    if altText.trim.isEmpty then
      Left(ContentError(s"Image missing required alt text: $url"))
    else
      Right(ContentImage(url, altText))
```

#### FormattedContent (Updated)

```scala
case class FormattedContent(
  textSpans: List[TextSpan],
  links: List[Link],
  codeBlocks: List[CodeBlock],
  contentImages: List[ContentImage]  // NEW
):
  // Word count excludes images
  def visibleWordCount: Int =
    textSpans.map(_.wordCount).sum

  // Line count excludes images and code blocks
  def visibleLineCount: Int =
    textSpans.map(_.lineCount).sum

  // Visual density check
  def visualDensityWarning: Option[ValidationError] =
    if contentImages.length >= 5 then
      Some(DensityWarning(s"Excessive visual density (${contentImages.length} images). Consider splitting."))
    else if contentImages.length >= 3 then
      Some(DensityWarning(s"High visual density (${contentImages.length} images)."))
    else
      None
```

#### Slide (Updated)

```scala
case class Slide(
  id: SlideId,
  templateName: String,
  slots: Map[String, FormattedContent],
  backgroundImage: Option[BackgroundImage]  // NEW
)

object Slide:
  def validated(
    id: SlideId,
    templateName: String,
    slots: Map[String, FormattedContent],
    backgroundImage: Option[BackgroundImage]
  ): Either[NonEmptyList[ValidationError], Slide] =
    val slide = Slide(id, templateName, slots, backgroundImage)

    // Structure validation
    val structureErrors = validateStructure(slide)

    // Content validation (includes image alt text, visual density)
    val contentErrors = if structureErrors.isEmpty then validateContent(slide) else Nil

    // Visual density warnings
    val densityWarnings = slots.values.flatMap(_.visualDensityWarning).toList

    val allErrors = structureErrors ++ contentErrors
    val allWarnings = densityWarnings  // Warnings don't block validation

    // Report warnings but don't fail validation
    if allWarnings.nonEmpty then
      println(allWarnings.map(_.displayMessage).mkString("\n"))

    NonEmptyList.fromList(allErrors) match
      case Some(errors) => Left(errors)
      case None => Right(slide)
```

## Examples

### Example 1: Content Slide with Content Images (Valid)

```markdown
---
template: content
---
## User Flow

The user journey has three stages:

![Login Screen](./screens/login.png)
![Dashboard View](./screens/dashboard.png)

Simple and intuitive navigation.
```

**Validation:**
- Alt text present for both images ✓
- Word count: 11 words (excluding images) ✓
- Line count: 4 lines (excluding images) ✓
- Visual density: 2 images ✓
- **Result**: ✓ Pass

### Example 2: Slide with Background Image (Theme)

**Theme JSON:**
```json
{
  "name": "corporate",
  "background": {
    "color": "#f8f9fa",
    "image": "./themes/corporate/watermark.png",
    "opacity": 0.05
  }
}
```

**Slide Markdown:**
```markdown
---
template: content
---
## Company Overview

We serve enterprise clients globally.
```

**Validation:**
- Background image from theme ✓
- No content images ✓
- **Result**: ✓ Pass

### Example 3: Slide with Template Background Override

```markdown
---
template: title
backgroundImage: ./images/hero-image.png
backgroundOpacity: 0.2
---
# Product Launch
## Q1 2025 Roadmap
```

**Validation:**
- Background image from template frontmatter ✓
- Overrides theme background (if any) ✓
- **Result**: ✓ Pass

### Example 4: Excessive Content Images (Warning)

```markdown
---
template: content
---
## Gallery

![Image 1](./img/1.png)
![Image 2](./img/2.png)
![Image 3](./img/3.png)
![Image 4](./img/4.png)
![Image 5](./img/5.png)
```

**Validation:**
- All images have alt text ✓
- Visual density: 5 images ⚠ Warning
- **Result**: ✓ Pass with warning

**Warning Message:**
```
⚠ Slide 3: Excessive visual density (5 images). Consider splitting across multiple slides.
```

### Example 5: Missing Alt Text (Error)

```markdown
---
template: content
---
## Diagram

![](./diagrams/architecture.png)
```

**Validation:**
- Image missing alt text ✗
- **Result**: ✗ Fail

**Error Message:**
```
✗ Slide 1: Image missing required alt text.
  Image: ./diagrams/architecture.png
  Fix: ![Describe the architecture diagram](./diagrams/architecture.png)
```

## Accessibility Considerations (PDR-005)

### Content Images: Alt Text Required

**Why**: Screen readers need descriptions of informational images

**Rule**: All content images must have non-empty alt text

**Example (Good):**
```markdown
![Bar chart showing 40% increase in revenue](./charts/revenue.png)
```

**Example (Bad):**
```markdown
![](./charts/revenue.png)  <!-- Missing alt text -->
```

### Background Images: Alt Text NOT Required

**Why**: Background images are decorative, not informational

**Rule**: Background images don't need alt text

**Rendering**: Background images use CSS `background-image`, not `<img>` tags

## Consequences

### Positive

- **Clear distinction**: Background vs content images have different rules
- **Flexibility**: Multiple content images allowed when needed
- **Accessibility**: Content images require alt text
- **Visual clarity**: Warnings prevent slide clutter

### Negative

- **Complexity**: Two image types with different validation rules
- **Learning curve**: Users must understand the distinction

### Mitigation

- **Clear documentation**: Explain when to use each image type
- **Validation messages**: Guide users to correct usage
- **Examples**: Provide templates for common scenarios

## References

- [US-005: Image Embedding](../CEREMONIES-v0.2.0.md#us-005-image-embedding)
- [PDR-001: Density Validation Limits](PDR-001-density-limits.md)
- [PDR-005: Accessibility Requirements](PDR-005-accessibility.md)
- [PDR-007: Theme JSON Schema](PDR-007-theme-schema.md)
- [WCAG 2.1: Non-Text Content](https://www.w3.org/WAI/WCAG21/Understanding/non-text-content.html)

## Revision History

- **2024-12-21**: Initial version (v0.2.0)
