# PDR-004: Template Constraints (Content Slide)

**Status**: Accepted
**Date**: 2024-12-20
**Decider**: Tony Moores (Product Owner)
**Consulted**: Tony Moores (Architect, Team)
**Related Ceremony**: US-002 (Content Slide Template), US-013 (Content Validation)

---

## Context

The **content slide template** is the workhorse of presentations - used for bullet points, explanations, key messages. Unlike the title slide (which is freeform), content slides need constraints to ensure readability.

**Business Question**: What constraints should content slides enforce?

**User Problems**:
1. **Heading too long**: Wraps to 3+ lines, dominates slide
2. **Body too complex**: Code blocks, images, tables make slide unreadable
3. **Inconsistent formatting**: Some slides text-only, others mixed media

**Constraints**:
- Content slides are for **text content** (bullet points, paragraphs)
- Specialized templates exist for other content types:
  - **Code slide**: Code blocks with syntax highlighting (v1.1)
  - **Image slide**: Large images with captions (v1.1)
  - **Quote slide**: Pull quotes with attribution (v1.2)
- Constraints must align with density limits (PDR-001)

---

## Decision

**Content Slide Template Constraints**:

### Heading Slot
- **Type**: Plain text (no markdown)
- **Max Length**: 80 characters
- **Rationale**: Headings should fit on 1-2 lines at 48px font
- **Enforcement**: Content validation ERROR (blocking)

### Body Slot
- **Type**: Markdown (formatted text)
- **Allowed Elements**:
  - ✅ Paragraphs
  - ✅ Bullet lists (unordered)
  - ✅ Numbered lists (ordered)
  - ✅ Inline formatting (bold, italics, inline code)
  - ✅ Links
- **Disallowed Elements**:
  - ❌ Code blocks (use code slide template in v1.1)
  - ❌ Images (use image slide template in v1.1)
  - ❌ Tables (use data slide template in v1.2)
  - ❌ Nested lists (>2 levels - readability issue)
- **Max Density**: 12 lines, 150 words (from PDR-001)
- **Enforcement**:
  - Disallowed elements → Content validation ERROR (blocking)
  - Density limits → Density validation WARNING (non-blocking)

**Rationale**:

### Why Disallow Code Blocks?
- Code blocks require monospace font (different sizing)
- Syntax highlighting needs theme support
- Code readability requires larger fonts (18-20px minimum)
- Better served by dedicated code slide template

### Why Disallow Images?
- Images need sizing/positioning (design concern)
- Accessibility requires alt text (specialized validation)
- Large images dominate slides (content slides are text-focused)
- Better served by dedicated image slide template

### Why Disallow Tables?
- Tables are data-heavy (different cognitive load)
- Table readability requires careful sizing (often too small)
- Better served by data slide template (future)

### Why Limit Heading to 80 Chars?
- 1920px width ÷ 48px font ≈ 40 chars/line
- 2 lines × 40 chars = 80 chars
- Longer headings wrap to 3+ lines (dominates slide)

---

## Consequences

### Positive

1. **Focused Content Slides**: Text-only keeps slides readable
2. **Specialized Templates**: Complex content gets appropriate templates
3. **Consistent Formatting**: All content slides have similar structure
4. **Validation Clarity**: Clear error messages ("use code slide for code blocks")
5. **Future Flexibility**: Can add code/image slides in v1.1 without breaking content slides

### Negative

1. **Author Friction**: "Why can't I put an image in a content slide?"
2. **Missing Templates**: v1.0 only has content + title (code/image slides in v1.1)
3. **Workaround Needed**: Authors must wait for v1.1 or use external images

### Mitigations

1. **Clear Error Messages**: Explain which template to use instead
2. **Fast Follow**: Prioritize code/image slide templates in v1.1 (4-6 weeks)
3. **Documentation**: Template usage guide with examples

---

## Alternatives Considered

### Alternative A: Allow Everything in Content Slides
**Rationale**: Let authors put any markdown in content slides
**Why Rejected**:
- No authoring rails (authors create unreadable slides)
- Code blocks at 24px font are unreadable (need 18-20px monospace)
- Images without sizing constraints break layouts
- Defeats purpose of MDSlides (opinionated, guided authoring)

### Alternative B: Stricter Constraints (No Lists)
**Constraints**: Only paragraphs, no lists
**Why Rejected**:
- Bullet points are standard in presentations (necessary)
- Numbered lists common for step-by-step content
- Too strict (frustrates authors)

### Alternative C: Allow Inline Images
**Constraints**: Allow images but not code blocks
**Why Rejected**:
- Inline images still need sizing (layout problem)
- Accessibility validation needed (alt text)
- Image slide template (v1.1) is better solution

### Alternative D: Allow Single-Level Lists Only
**Constraints**: Lists allowed, but no nesting
**Why Rejected**:
- 2-level nesting is common and readable (e.g., main points with sub-points)
- 3+ levels are problematic (this decision allows 2 levels, blocks 3+)

---

## Implementation Notes

### Template Definition

```yaml
# templates/content.yaml
name: content
description: Text content with bullet points or paragraphs
slots:
  - name: heading
    type: text
    required: true
    constraints:
      max_chars: 80

  - name: body
    type: markdown
    required: true
    constraints:
      allowed_elements:
        - paragraph
        - bullet_list
        - ordered_list
        - emphasis
        - strong
        - inline_code
        - link
      disallowed_elements:
        - code_block
        - image
        - table
        - heading  # No nested headings
      max_list_nesting: 2
```

### Content Validation Logic

```scala
def validateContentSlide(slide: Slide): Either[NonEmptyList[ContentError], Slide] = {
  val errors = List.newBuilder[ContentError]

  // Validate heading length
  slide.getSlot("heading").foreach { heading =>
    if (heading.length > 80) {
      errors += ContentError(
        slide.id,
        s"Heading exceeds maximum length (${heading.length} chars, limit: 80). " +
        "Shorten heading or split into multiple slides."
      )
    }
  }

  // Validate body disallowed elements
  slide.getSlot("body").foreach { body =>
    val bodyNodes = parseMarkdown(body)

    // Check for code blocks
    if (bodyNodes.exists(_.isInstanceOf[FencedCodeBlock])) {
      errors += ContentError(
        slide.id,
        "Code blocks not allowed in content slides. Use code slide template instead (available in v1.1)."
      )
    }

    // Check for images
    if (bodyNodes.exists(_.isInstanceOf[Image])) {
      errors += ContentError(
        slide.id,
        "Images not allowed in content slides. Use image slide template instead (available in v1.1)."
      )
    }

    // Check for tables
    if (bodyNodes.exists(_.isInstanceOf[TableBlock])) {
      errors += ContentError(
        slide.id,
        "Tables not allowed in content slides. Use data slide template instead (available in v1.2)."
      )
    }

    // Check for nested lists (>2 levels)
    if (hasDeepNesting(bodyNodes, maxDepth = 2)) {
      errors += ContentError(
        slide.id,
        "Lists nested more than 2 levels deep are not allowed (readability issue). " +
        "Flatten list structure or split into multiple slides."
      )
    }
  }

  NonEmptyList.fromList(errors.result()) match {
    case Some(errs) => Left(errs)
    case None       => Right(slide)
  }
}

def hasDeepNesting(nodes: List[Node], maxDepth: Int): Boolean = {
  def depth(node: Node, level: Int): Int = {
    node match {
      case list: BulletList =>
        val childDepths = list.children.map(depth(_, level + 1))
        if (childDepths.isEmpty) level else childDepths.max
      case list: OrderedList =>
        val childDepths = list.children.map(depth(_, level + 1))
        if (childDepths.isEmpty) level else childDepths.max
      case _ => level
    }
  }

  nodes.exists(depth(_, 1) > maxDepth)
}
```

### Error Messages

**Heading Too Long**:
```
❌ Content Validation Failed

Slide 5:
  ❌ Heading exceeds maximum length (95 chars, limit: 80).
     → Current: "This is an extremely long heading that wraps to multiple lines and dominates the slide"
     → Suggestion: "Long Headings Reduce Readability"
```

**Code Block Detected**:
```
❌ Content Validation Failed

Slide 7:
  ❌ Code blocks not allowed in content slides.
     → Use code slide template instead (available in v1.1).
     → Workaround for v1.0: Use inline code (`code`) or external tool for code slides.
```

**Image Detected**:
```
❌ Content Validation Failed

Slide 10:
  ❌ Images not allowed in content slides.
     → Use image slide template instead (available in v1.1).
     → Workaround for v1.0: Create image slide using PowerPoint, export to PDF.
```

**Nested Lists (>2 Levels)**:
```
❌ Content Validation Failed

Slide 12:
  ❌ Lists nested more than 2 levels deep are not allowed.
     → Nested lists are hard to read on slides.
     → Flatten structure:
       BEFORE (3 levels):
       - Main Point
         - Sub-point
           - Sub-sub-point  ← Too deep

       AFTER (2 levels):
       - Main Point
         - Sub-point A
         - Sub-point B
```

---

## User Experience

### Success Path (Valid Content Slide)

```markdown
# Key Metrics

- Revenue: $1.2M (+15% YoY)
- Customer Growth: 500 new customers
- **Market Share**: 12% (industry leading)

Next steps:
1. Expand to EMEA region
2. Launch enterprise tier
```

**Result**: Valid (plain text heading, markdown body with lists and inline formatting)

### Failure Path (Code Block)

```markdown
# API Usage

To authenticate, use this code:

​```python
import requests
response = requests.get('https://api.example.com', headers={'Authorization': 'Bearer token'})
​```
```

**Result**: Content validation ERROR
```
❌ Content Validation Failed

Slide 3:
  ❌ Code blocks not allowed in content slides.
     → Use code slide template instead (available in v1.1).
```

**User options**:
1. Wait for v1.1 code slide template
2. Remove code block, describe in text instead
3. Use external tool (PowerPoint) for code slides

### Failure Path (Image)

```markdown
# Architecture Overview

![System Diagram](diagrams/architecture.png)

The system consists of 3 microservices...
```

**Result**: Content validation ERROR
```
❌ Content Validation Failed

Slide 5:
  ❌ Images not allowed in content slides.
     → Use image slide template instead (available in v1.1).
```

---

## Future Templates (v1.1+)

### Code Slide Template (v1.1)
```yaml
name: code
slots:
  - name: heading
    type: text
    required: true
    constraints:
      max_chars: 60

  - name: code
    type: code_block
    required: true
    constraints:
      max_lines: 20
      allowed_languages: [python, java, scala, javascript, bash]
      syntax_highlighting: true

  - name: caption
    type: text
    required: false
```

### Image Slide Template (v1.1)
```yaml
name: image
slots:
  - name: heading
    type: text
    required: true

  - name: image
    type: image
    required: true
    constraints:
      max_width: 1600px
      max_height: 900px
      allowed_formats: [jpg, png, svg]
      require_alt_text: true

  - name: caption
    type: text
    required: false
    constraints:
      max_chars: 200
```

---

## Related Ceremonies

- **US-002**: Content Slide Template (Three Amigos + Example Mapping)
- **US-013**: Content Validation (Three Amigos + Example Mapping)
- **Domain Modeling**: Template aggregate, Slot value objects

---

## Related Governance

- **PDR-001**: Density Validation Limits (12 lines, 150 words applies to content slide body)
- **ADR-008**: Slot-Based Content Model (templates define slot constraints)
- **POL-001**: Ubiquitous Language (use "Content Slide" not "Text Slide" or "Bullet Slide")

---

**Decision Owner**: Tony Moores (Product Owner)
**Business Impact**: High (defines core template constraints)
**User Impact**: High (affects most slides in typical presentations)
**Roadmap Impact**: Medium (drives need for code/image slide templates in v1.1)
**Reversibility**: Low (relaxing constraints later is non-breaking, but stricter constraints break existing decks)
