package com.tjmsolutions.mdslides.infrastructure.parser

import munit.FunSuite
import com.tjmsolutions.mdslides.domain.{DiagramElement, MermaidDiagram, CodeBlockElement}

/**
 * Tests for Mermaid diagram parsing in FlexmarkAdapter (v2.0.0).
 *
 * Tests verify:
 * - ```mermaid code fences are parsed as DiagramElement
 * - Regular ```language code fences remain as CodeBlockElement
 * - Diagram type is detected correctly
 * - Diagrams are added to content in source order
 *
 * Related Governance:
 * - v2.0.0: Mermaid Diagram Support (US-022)
 * - Example Mapping: Scenarios 8-10 (parsing)
 */
class FlexmarkMermaidSpec extends FunSuite:

  // Test parsing ```mermaid code fence
  test("parse ```mermaid code fence as DiagramElement"):
    val markdown = """
```mermaid
graph TD
    A[Start] --> B[End]
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(formatted.content.size, 1, "Should have 1 content element")
    formatted.content.head match
      case DiagramElement(diagram) =>
        assert(diagram.source.contains("graph TD"), s"Expected Mermaid source, got: ${diagram.source}")
        assert(diagram.source.contains("A[Start] --> B[End]"), "Diagram should contain nodes")
        assertEquals(diagram.diagramType, "flowchart")
        assertEquals(diagram.altText, None, "Alt text not set from code fence")
      case other =>
        fail(s"Expected DiagramElement, got: $other")

  // Test that regular code blocks still work
  test("parse ```python code fence as CodeBlockElement"):
    val markdown = """
```python
def hello():
    print("Hello")
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    assertEquals(formatted.content.size, 1)
    formatted.content.head match
      case CodeBlockElement(block) =>
        assert(block.code.contains("def hello()"), s"Expected Python code, got: ${block.code}")
        assertEquals(block.language, Some("python"))
      case other =>
        fail(s"Expected CodeBlockElement, got: $other")

  // Test sequence diagram parsing
  test("parse ```mermaid sequence diagram"):
    val markdown = """
```mermaid
sequenceDiagram
    Alice->>Bob: Hello
    Bob-->>Alice: Hi
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    formatted.content.head match
      case DiagramElement(diagram) =>
        assertEquals(diagram.diagramType, "sequence")
        assert(diagram.source.contains("Alice->>Bob"))
      case _ =>
        fail("Expected DiagramElement")

  // Test class diagram parsing
  test("parse ```mermaid class diagram"):
    val markdown = """
```mermaid
classDiagram
    class Animal {
        +String name
    }
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    formatted.content.head match
      case DiagramElement(diagram) =>
        assertEquals(diagram.diagramType, "class")
      case _ =>
        fail("Expected DiagramElement")

  // Test mixed content (text + diagram + text)
  test("parse mixed content with Mermaid diagram"):
    val markdown = """
Here is a diagram:

```mermaid
graph TD
    A --> B
```

And here is more text.
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    assert(formatted.content.size >= 3, s"Expected at least 3 elements (para, diagram, para), got: ${formatted.content.size}")

    // Find the diagram element
    val diagramOpt = formatted.content.collectFirst { case DiagramElement(d) => d }
    assert(diagramOpt.isDefined, "Should contain a DiagramElement")
    assertEquals(diagramOpt.get.diagramType, "flowchart")

  // Test multiple diagrams in one slide
  test("parse multiple Mermaid diagrams in one slide"):
    val markdown = """
First diagram:

```mermaid
graph TD
    A --> B
```

Second diagram:

```mermaid
sequenceDiagram
    A->>B: Hi
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    val diagrams = formatted.content.collect { case DiagramElement(d) => d }
    assertEquals(diagrams.size, 2, "Should have 2 diagrams")
    assertEquals(diagrams(0).diagramType, "flowchart")
    assertEquals(diagrams(1).diagramType, "sequence")

  // Test diagram with indentation preserved
  test("preserve indentation in Mermaid source"):
    val markdown = """
```mermaid
graph TD
    A[Start] --> B{Decision}
    B -->|Yes| C[Good]
    B -->|No| D[Bad]
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    formatted.content.head match
      case DiagramElement(diagram) =>
        assert(diagram.source.contains("    A[Start]"), "Should preserve indentation")
        assert(diagram.source.contains("    B -->|Yes|"), "Should preserve indentation")
      case _ =>
        fail("Expected DiagramElement")

  // Test empty mermaid block (edge case)
  test("parse empty ```mermaid block"):
    val markdown = """
```mermaid
```
""".trim

    val formatted = FlexmarkAdapter.parseInlineFormatting(markdown)

    formatted.content.headOption match
      case Some(DiagramElement(diagram)) =>
        assertEquals(diagram.source.trim, "")
        assertEquals(diagram.diagramType, "unknown")
      case _ =>
        // Empty mermaid might be skipped or treated as empty diagram
        // Either behavior is acceptable
        ()

end FlexmarkMermaidSpec
