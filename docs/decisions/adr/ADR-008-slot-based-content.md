# ADR-008: Slot-Based Content Model

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-001 (Title Slide), US-002 (Content Slide), Domain Modeling

---

## Context

Slide content needs structured authoring to ensure:
- **Template Compliance**: Content matches template expectations
- **Validation**: Can enforce constraints per slot (e.g., title max 80 chars)
- **Rendering**: Know where to place content in HTML output
- **Authoring Rails**: Authors guided to create well-structured slides

**Key Questions**:
1. How to map Markdown content to slide structure?
2. How to enforce template-specific constraints?
3. How to support multiple templates (title, content, future: image, code)?
4. How to prevent authors from creating invalid slides?

**Constraints**:
- Must be parseable from Markdown (no proprietary syntax)
- Must support template evolution (v1.1 adds new templates)
- Must enable compile-time validation where possible

---

## Decision

**Slot-Based Content Model**: Templates define named slots, content maps to slots via Markdown structure:

```yaml
# Template Definition (templates/title.yaml)
name: title
slots:
  - name: title
    type: text
    required: true
    constraints:
      max_chars: 80

  - name: subtitle
    type: text
    required: false
    constraints:
      max_chars: 120

  - name: author
    type: text
    required: false
    constraints:
      max_chars: 60
```

**Markdown Mapping**:
```markdown
# My Presentation Title        ← maps to "title" slot (first H1)
## Subtitle Goes Here          ← maps to "subtitle" slot (first H2 after title)
Author: John Doe               ← maps to "author" slot (paragraph starting with "Author:")
```

**Domain Model**:
```scala
case class Template(
  name: String,
  slots: List[SlotDefinition]
)

case class SlotDefinition(
  name: String,
  slotType: SlotType,
  required: Boolean,
  constraints: SlotConstraints
)

enum SlotType {
  case Text
  case Markdown
  case Image
  case Code
}

case class SlotConstraints(
  maxChars: Option[Int] = None,
  maxLines: Option[Int] = None,
  maxWords: Option[Int] = None,
  allowedLanguages: Option[List[String]] = None  // For code slots
)

case class Slide(
  id: Int,
  template: String,
  slots: Map[String, String]  // Slot name → content
) {
  def getSlot(name: String): Option[String] = slots.get(name)
}
```

**Validation Pipeline**:
1. **Structure Validation**: Check all required slots present, types match
2. **Content Validation**: Check slot-specific constraints (max chars, etc.)

---

## Consequences

### Positive

1. **Authoring Rails**: Templates guide authors to create well-structured slides
2. **Template Evolution**: Easy to add new templates (image, code, quote, etc.)
3. **Validation**: Constraints tied to slots (title has different limits than body)
4. **Type Safety**: Slot definitions prevent invalid content mapping
5. **Rendering**: Renderer knows exactly where to place content
6. **Flexibility**: Same slide type (e.g., content) can have variations via slots
7. **Ubiquitous Language**: Slots reflect domain concepts (title, body, author, etc.)

### Negative

1. **Parsing Complexity**: Must infer slot mapping from Markdown structure
2. **Limited Flexibility**: Authors constrained by template structure
3. **Learning Curve**: Authors must understand slot mapping rules

### Risks

1. **Risk**: Markdown structure ambiguous (which paragraph is which slot?)
   - **Mitigation**: Clear mapping rules documented, strict parsing
2. **Risk**: Templates too rigid (authors want custom layouts)
   - **Mitigation**: v1.1 supports custom templates, v1.0 focuses on common cases
3. **Risk**: Slot constraints too strict (frustrating authors)
   - **Mitigation**: Density constraints are warnings, not errors (ADR-002)

---

## Alternatives Considered

### Alternative A: Free-Form Markdown (No Templates)
```markdown
# Slide 1

Anything goes here.

---

# Slide 2

More free-form content.
```
**Why Rejected**:
- No validation (can't enforce "fits on slide")
- No authoring rails (authors create bad slides)
- Rendering ambiguous (where does content go?)
- Doesn't support MDSlides' value proposition (opinionated, guided authoring)

### Alternative B: YAML Frontmatter per Slide
```markdown
---
template: title
title: My Presentation
subtitle: A Great Talk
author: John Doe
---

---
template: content
heading: Key Points
body: |
  - Point 1
  - Point 2
---
```
**Why Rejected**:
- Verbose (every slide needs frontmatter)
- Doesn't feel like Markdown (feels like config file)
- Hard to preview in GitHub (YAML not rendered)
- Authoring overhead too high

### Alternative C: HTML Data Attributes
```markdown
<div data-slot="title">My Presentation</div>
<div data-slot="subtitle">A Great Talk</div>
```
**Why Rejected**:
- Not Markdown (defeats purpose)
- Requires HTML knowledge
- Breaks GitHub preview
- Verbose, hard to read

### Alternative D: Magic Comments
```markdown
<!-- slot: title -->
# My Presentation

<!-- slot: subtitle -->
## A Great Talk
```
**Why Rejected**:
- Verbose (comment for every slot)
- Invisible in GitHub preview
- Easy to forget comments
- Not intuitive

### Alternative E: Positional Slots (First H1 = Title)
**Why ACCEPTED**:
- Natural Markdown structure
- Readable without MDSlides (GitHub preview works)
- Intuitive for authors
- Minimal syntax

---

## Implementation Notes

### Slot Mapping Rules

**Title Slide** (template: `title`):
```markdown
# Title Text              ← Slot "title" (first H1, required)
## Subtitle Text          ← Slot "subtitle" (first H2 after title, optional)
Author: John Doe          ← Slot "author" (paragraph starting "Author:", optional)
```

**Content Slide** (template: `content`):
```markdown
# Heading Text           ← Slot "heading" (first H1, required)

Body content here        ← Slot "body" (all paragraphs after heading, required)
with multiple paragraphs.

- Lists are fine
- Code blocks too
```

**Parsing Logic**:
```scala
def parseSlide(nodes: List[Node]): Slide = {
  val firstHeading = nodes.collectFirst { case h: Heading => h }
  val template = inferTemplate(nodes)  // Heuristic: presence of H2 → title, else content

  template match {
    case "title" =>
      Slide(
        template = "title",
        slots = Map(
          "title" -> extractH1(nodes),
          "subtitle" -> extractH2(nodes).getOrElse(""),
          "author" -> extractAuthor(nodes).getOrElse("")
        )
      )

    case "content" =>
      Slide(
        template = "content",
        slots = Map(
          "heading" -> extractH1(nodes),
          "body" -> extractBody(nodes)
        )
      )
  }
}

def extractH1(nodes: List[Node]): String = {
  nodes.collectFirst { case h: Heading if h.getLevel == 1 => h.getText }.getOrElse("")
}

def extractH2(nodes: List[Node]): Option[String] = {
  nodes.collectFirst { case h: Heading if h.getLevel == 2 => h.getText }
}

def extractBody(nodes: List[Node]): String = {
  // All nodes except first heading
  nodes.dropWhile(!_.isInstanceOf[Heading]).drop(1).map(renderNode).mkString("\n")
}
```

### Template Inference

```scala
def inferTemplate(nodes: List[Node]): String = {
  val hasH2 = nodes.exists(n => n.isInstanceOf[Heading] && n.asInstanceOf[Heading].getLevel == 2)
  val hasAuthor = nodes.exists(n => n.isInstanceOf[Paragraph] && n.getText.startsWith("Author:"))

  if (hasH2 || hasAuthor) "title" else "content"
}
```

### Validation Against Template

```scala
def validateSlideStructure(slide: Slide, templates: Map[String, Template]): Either[StructureError, Slide] = {
  templates.get(slide.template) match {
    case None =>
      Left(StructureError(slide.id, s"Unknown template: ${slide.template}"))

    case Some(template) =>
      val errors = template.slots.flatMap { slotDef =>
        slide.getSlot(slotDef.name) match {
          case None if slotDef.required =>
            Some(StructureError(slide.id, s"Missing required slot '${slotDef.name}'"))
          case Some(content) if !matchesType(content, slotDef.slotType) =>
            Some(StructureError(slide.id, s"Slot '${slotDef.name}' type mismatch"))
          case _ =>
            None
        }
      }

      NonEmptyList.fromList(errors) match {
        case Some(errs) => Left(errs.head) // Simplified for example
        case None       => Right(slide)
      }
  }
}
```

### Content Constraint Validation

```scala
def validateSlotConstraints(slide: Slide, template: Template): Either[ContentError, Slide] = {
  val errors = template.slots.flatMap { slotDef =>
    slide.getSlot(slotDef.name).flatMap { content =>
      slotDef.constraints.maxChars.flatMap { maxChars =>
        if (content.length > maxChars) {
          Some(ContentError(
            slide.id,
            s"Slot '${slotDef.name}' exceeds max chars (${content.length} > $maxChars)"
          ))
        } else None
      }
    }
  }

  NonEmptyList.fromList(errors) match {
    case Some(errs) => Left(errs.head)
    case None       => Right(slide)
  }
}
```

### Rendering with Slots

```scala
def renderSlide(slide: Slide, theme: Theme): Html = {
  slide.template match {
    case "title" =>
      div(cls := "slide title-slide")(
        h1(slide.getSlot("title").getOrElse("")),
        slide.getSlot("subtitle").map(s => h2(s)),
        slide.getSlot("author").map(a => p(cls := "author")(a))
      )

    case "content" =>
      div(cls := "slide content-slide")(
        h2(slide.getSlot("heading").getOrElse("")),
        div(cls := "body")(
          raw(renderMarkdown(slide.getSlot("body").getOrElse("")))
        )
      )

    case unknown =>
      div(cls := "slide error-slide")(
        p(s"Unknown template: $unknown")
      )
  }
}
```

### Future Templates (v1.1+)

**Image Slide**:
```yaml
# templates/image.yaml
name: image
slots:
  - name: heading
    type: text
    required: true
    constraints:
      max_chars: 60

  - name: image
    type: image
    required: true
    constraints:
      allowed_extensions: [jpg, png, svg]

  - name: caption
    type: text
    required: false
    constraints:
      max_chars: 200
```

**Code Slide**:
```yaml
# templates/code.yaml
name: code
slots:
  - name: heading
    type: text
    required: true

  - name: code
    type: code
    required: true
    constraints:
      max_lines: 20
      allowed_languages: [scala, python, javascript, java]

  - name: notes
    type: markdown
    required: false
```

---

**ADR Type**: Domain Model Design
**Impact**: Domain layer (all slide/template code), parser, validator, renderer
**Reversibility**: Low (fundamental to domain model)
**Validation**: Validated in ceremonies US-001, US-002
**Key Decision Rationale**: Slot-based model provides authoring rails while preserving Markdown simplicity
**Related Policies**: POL-001 (Ubiquitous Language - slot terminology)
