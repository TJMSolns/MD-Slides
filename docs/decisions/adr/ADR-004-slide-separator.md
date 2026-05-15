# ADR-004: Slide Separator Design

**Status**: Accepted
**Date**: 2024-12-20
**Deciders**: Tony Moores (Architect, Bench Developer)
**Related Ceremony**: US-003 (Slide Separation Parsing)

---

## Context

Markdown slide decks need a way to separate individual slides. The separator must be:
- **Unambiguous**: Clear boundary between slides (not confused with content)
- **Intuitive**: Authors can understand without documentation
- **Compatible**: Works with existing Markdown tools (GitHub preview, VS Code)
- **Parseable**: Flexmark can detect it reliably

**Constraints**:
- Must be valid CommonMark/GFM syntax
- Must not break GitHub Markdown preview
- Must not conflict with slide content (e.g., code blocks)
- Should be visually distinct in raw Markdown

---

## Decision

Use **`---` (triple hyphen)** as the slide separator, which Flexmark parses as a **ThematicBreak** node.

**Markdown Syntax**:
```markdown
# Slide 1 Title

Content for slide 1.

---

# Slide 2 Title

Content for slide 2.

---
```

**Flexmark AST**:
```
Document
  ├─ Heading (level 1): "Slide 1 Title"
  ├─ Paragraph: "Content for slide 1."
  ├─ ThematicBreak           <--- Slide separator
  ├─ Heading (level 1): "Slide 2 Title"
  ├─ Paragraph: "Content for slide 2."
  └─ ThematicBreak
```

**Parsing Logic**:
- Split AST on `ThematicBreak` nodes
- Each segment between breaks = one slide
- First segment (before first break) = slide 1

---

## Consequences

### Positive

1. **GitHub Compatible**: `---` renders as `<hr>` in GitHub preview (visual separator)
2. **Unambiguous**: ThematicBreak is a distinct AST node (not confused with content)
3. **Intuitive**: Authors familiar with Markdown recognize `---` as a separator
4. **Simple Parsing**: Flexmark provides `ThematicBreak` node directly (no regex needed)
5. **Future-Proof**: CommonMark spec guarantees `---` = ThematicBreak
6. **VS Code Support**: Markdown preview shows `---` as horizontal rule

### Negative

1. **Conflict Risk**: Authors might use `---` in content (e.g., within code blocks)
   - **Mitigation**: Inside code blocks, `---` is not parsed as ThematicBreak (Flexmark handles this)
2. **Hidden Separator**: In some Markdown editors, `---` might render invisibly
   - **Mitigation**: Use Markdown-aware editors (VS Code, Obsidian) that show raw syntax

### Risks

1. **Risk**: Author forgets separator → all slides merged into one
   - **Mitigation**: Structure validation checks slide count (min 1, max 200)
2. **Risk**: Flexmark update changes ThematicBreak parsing
   - **Mitigation**: Pin Flexmark version (ADR-001), comprehensive test suite

---

## Alternatives Considered

### Alternative A: `<!-- SLIDE -->` (HTML Comment)
**Syntax**:
```markdown
# Slide 1
<!-- SLIDE -->
# Slide 2
```
**Why Rejected**:
- Verbose (not intuitive)
- Invisible in GitHub preview (no visual separator)
- Requires custom Flexmark extension (more code to maintain)

### Alternative B: `===` (Setext Heading Underline)
**Syntax**:
```markdown
# Slide 1

===

# Slide 2
```
**Why Rejected**:
- `===` under text creates heading (ambiguous)
- Flexmark parses `===` differently depending on context
- Confusing for authors (looks like heading, acts as separator)

### Alternative C: Two Blank Lines
**Syntax**:
```markdown
# Slide 1


# Slide 2
```
**Why Rejected**:
- Fragile (easy to accidentally add/remove blank line)
- Not visually distinct (hard to see in raw Markdown)
- Conflicts with intentional paragraph spacing

### Alternative D: Custom Delimiter (e.g., `:::slide`)
**Syntax**:
```markdown
# Slide 1
:::slide
# Slide 2
```
**Why Rejected**:
- Not standard Markdown (breaks GitHub preview)
- Requires custom Flexmark extension
- Authors must learn proprietary syntax

### Alternative E: Heading Level 1 as Separator
**Syntax**:
```markdown
# Slide 1 Title
Content
# Slide 2 Title
Content
```
**Why Rejected**:
- Not all slides have headings (e.g., image slides)
- Forces slide structure (inflexible)
- Can't have multiple `<h1>` in one slide
- Marp uses this (we want different approach)

---

## Implementation Notes

### Parsing Algorithm

```scala
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node

def parseSlides(markdown: String): List[List[Node]] = {
  val parser = Parser.builder().build()
  val document = parser.parse(markdown)

  // Collect all nodes, split on ThematicBreak
  val nodes = collectNodes(document)
  splitOnThematicBreak(nodes)
}

def collectNodes(parent: Node): List[Node] = {
  val buffer = List.newBuilder[Node]
  var child = parent.getFirstChild
  while (child != null) {
    buffer += child
    child = child.getNext
  }
  buffer.result()
}

def splitOnThematicBreak(nodes: List[Node]): List[List[Node]] = {
  nodes.foldLeft(List(List.empty[Node])) { (slides, node) =>
    node match {
      case _: ThematicBreak =>
        // Start new slide
        List.empty[Node] :: slides
      case other =>
        // Add to current slide
        val currentSlide = slides.head
        (currentSlide :+ other) :: slides.tail
    }
  }.reverse.filterNot(_.isEmpty)
}
```

### Edge Cases

**Case 1: No separators** (single-slide deck)
```markdown
# Only Slide
Content here.
```
**Result**: 1 slide (valid)

**Case 2: Trailing separator**
```markdown
# Slide 1
---
# Slide 2
---
```
**Result**: 2 slides (trailing `---` ignored - creates empty slide, filtered out)

**Case 3: Leading separator**
```markdown
---
# Slide 1
```
**Result**: 1 slide (leading `---` creates empty slide, filtered out)

**Case 4: Multiple consecutive separators**
```markdown
# Slide 1
---
---
---
# Slide 2
```
**Result**: 2 slides (empty slides between separators filtered out)

**Case 5: Separator in code block**
```markdown
# Slide 1

​```yaml
key: value
---
another: value
​```

---

# Slide 2
```
**Result**: 2 slides (Flexmark doesn't parse `---` inside code block as ThematicBreak)

### Validation Rules

From US-011 (Structure Validation):
- Deck must have 1-200 slides (min/max)
- Empty slides (no content after separator) rejected
- Each slide must bind to a template

### Testing Strategy

```scala
class SlideParsingSpec extends munit.FunSuite {
  test("single separator creates two slides") {
    val markdown = """
      |# Slide 1
      |---
      |# Slide 2
    """.stripMargin

    val slides = parseSlides(markdown)
    assertEquals(slides.length, 2)
  }

  test("no separator creates one slide") {
    val markdown = "# Only Slide"
    val slides = parseSlides(markdown)
    assertEquals(slides.length, 1)
  }

  test("separator in code block ignored") {
    val markdown = """
      |# Slide 1
      |​```
      |---
      |​```
    """.stripMargin

    val slides = parseSlides(markdown)
    assertEquals(slides.length, 1)
  }

  test("trailing separator ignored") {
    val markdown = """
      |# Slide 1
      |---
    """.stripMargin

    val slides = parseSlides(markdown)
    assertEquals(slides.length, 1)
  }
}
```

### Flexmark Configuration

```scala
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.tables.TablesExtension

val options = new MutableDataSet()
  .set(Parser.EXTENSIONS, java.util.Arrays.asList(
    TablesExtension.create(),
    StrikethroughExtension.create()
  ))

val parser = Parser.builder(options).build()
```

**Note**: ThematicBreak parsing is part of CommonMark core (no extension needed).

---

**ADR Type**: Technical Implementation
**Impact**: Parser module (infrastructure layer)
**Reversibility**: Low (changing separator breaks all existing decks)
**Validation**: Validated in ceremony US-003
**Key Decision Rationale**: `---` is CommonMark standard, GitHub compatible, unambiguous in AST
