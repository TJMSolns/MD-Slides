package com.tjmsolutions.mdslides.infrastructure.validation

import munit.FunSuite
import com.tjmsolutions.mdslides.domain._
import com.tjmsolutions.mdslides.infrastructure.parser.FlexmarkAdapter

/**
 * Tests for Template-Specific Content Validation (v2.0.0).
 *
 * Tests verify REAL validation behavior using actual parsed content:
 * - Diagram template MUST contain at least one Mermaid diagram (Scenario 17)
 * - Other templates don't have diagram requirements
 * - Validation errors are meaningful and actionable
 *
 * Architecture:
 * - Domain validation operates on FormattedContent (parsed content)
 * - Infrastructure tests integrate parsing + validation
 * - Tests use real FlexmarkAdapter to parse markdown
 *
 * Related Governance:
 * - v2.0.0: Additional Templates
 * - Example Mapping: Scenario 17 (diagram validation)
 * - ADR-002: Validation Pipeline
 */
class TemplateContentValidationSpec extends FunSuite:

  // Example Mapping Scenario 17: Diagram template requires Mermaid diagram
  test("diagram template without Mermaid diagram fails validation"):
    // Create slide with diagram template but no Mermaid content
    val slide = Slide(
      id = SlideId.unsafe(1),  // Use unsafe for tests
      templateName = "diagram",
      slots = Map(
        "heading" -> "Architecture Overview",
        "caption" -> "System components"
      )
    )

    // Parse slots using FlexmarkAdapter (REAL parsing)
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify test setup: no Mermaid diagrams present
    val hasMermaidDiagram = parsedSlots.values.exists(_.content.exists {
      case DiagramElement(_) => true
      case _ => false
    })
    assert(!hasMermaidDiagram, "Test setup: slide should NOT have Mermaid diagram")

    // Run template-specific validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should have validation error
    assertEquals(errors.size, 1, "Should have 1 validation error")

    val error = errors.head
    error match
      case ValidationError.ContentError(_, _, msg, _) =>
        assert(msg.contains("Mermaid"), "Error should mention Mermaid")
        assert(msg.contains("```mermaid"), "Error should show syntax")
        assert(msg.contains("at least one"), "Error should specify quantity")
      case other =>
        fail(s"Expected ContentError, got: $other")

  // Example Mapping Scenario 17: Diagram template WITH Mermaid diagram passes validation
  test("diagram template with Mermaid diagram in body passes validation"):
    val mermaidSource = """graph TD
      A[Start] --> B[End]"""

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "diagram",
      slots = Map(
        "heading" -> "Architecture",
        "body" -> s"```mermaid\n$mermaidSource\n```"
      )
    )

    // Parse slots using FlexmarkAdapter (REAL parsing)
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify test setup: Mermaid diagram is present
    val bodyContent = parsedSlots("body")
    val hasMermaidDiagram = bodyContent.content.exists {
      case DiagramElement(diagram) =>
        assertEquals(diagram.diagramType, "flowchart", s"Expected flowchart, got ${diagram.diagramType}")
        true
      case _ => false
    }
    assert(hasMermaidDiagram, "Body should contain Mermaid diagram")

    // Run template-specific validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should have NO validation errors
    assertEquals(errors.size, 0, s"Should have no validation errors, got: $errors")

  // Diagram template can have Mermaid in ANY slot (not just body)
  test("diagram template can have Mermaid in heading slot"):
    val mermaidSource = """graph LR
      A --> B"""

    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "diagram",
      slots = Map(
        "heading" -> s"Diagram\n\n```mermaid\n$mermaidSource\n```"
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify Mermaid is in heading
    val headingContent = parsedSlots("heading")
    val hasMermaidDiagram = headingContent.content.exists {
      case DiagramElement(_) => true
      case _ => false
    }
    assert(hasMermaidDiagram, "Heading can contain Mermaid diagram")

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should pass validation
    assertEquals(errors.size, 0, s"Should have no validation errors, got: $errors")

  // Closing template doesn't require Mermaid diagram
  test("closing template without Mermaid diagram is valid"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "closing",
      slots = Map(
        "heading" -> "Thank You",
        "body" -> "Questions?\n\nContact: tony@example.com"
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify NO Mermaid diagram (and that's OK for closing template)
    val hasMermaidDiagram = parsedSlots.values.exists(_.content.exists {
      case DiagramElement(_) => true
      case _ => false
    })
    assert(!hasMermaidDiagram, "Closing template doesn't need Mermaid diagram")

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should pass validation
    assertEquals(errors.size, 0, s"Closing template should be valid without diagrams, got: $errors")

  // Section-title template doesn't require Mermaid diagram
  test("section-title template without Mermaid diagram is valid"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "section-title",
      slots = Map(
        "heading" -> "Part 2: Implementation",
        "body" -> "Deep dive into the architecture"
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify NO Mermaid diagram
    val hasMermaidDiagram = parsedSlots.values.exists(_.content.exists {
      case DiagramElement(_) => true
      case _ => false
    })
    assert(!hasMermaidDiagram, "Section-title doesn't need Mermaid diagram")

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should pass validation
    assertEquals(errors.size, 0, s"Section-title should be valid without diagrams, got: $errors")

  // Test multiple diagrams in one slide (should pass)
  test("diagram template with multiple Mermaid diagrams is valid"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "diagram",
      slots = Map(
        "heading" -> "Multiple Views",
        "body" -> """
```mermaid
graph TD
  A --> B
```

```mermaid
sequenceDiagram
  A->>B: Hello
```
"""
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Verify multiple diagrams are present
    val bodyContent = parsedSlots("body")
    val diagrams = bodyContent.content.collect {
      case DiagramElement(d) => d
    }
    assertEquals(diagrams.size, 2, "Should have 2 Mermaid diagrams")

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should pass validation
    assertEquals(errors.size, 0, s"Multiple diagrams should be valid, got: $errors")

  // Content template doesn't require Mermaid (and shouldn't fail if it has one)
  test("content template with Mermaid diagram is valid"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "content",
      slots = Map(
        "heading" -> "Architecture",
        "body" -> """
Some text explaining the architecture.

```mermaid
graph TD
  A --> B
```
"""
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Content template doesn't require OR prohibit diagrams
    assertEquals(errors.size, 0, s"Content template can contain diagrams, got: $errors")

  // Test empty diagram template (no content at all)
  test("diagram template with no content fails validation"):
    val slide = Slide(
      id = SlideId.unsafe(1),
      templateName = "diagram",
      slots = Map(
        "heading" -> "Empty Diagram"
        // No body or other slots
      )
    )

    // Parse slots
    val parsedSlots = slide.slots.map { case (slotName, content) =>
      slotName -> FlexmarkAdapter.parseInlineFormatting(content)
    }

    // Run validation
    val errors = Slide.validateTemplateSpecificContent(slide, parsedSlots)

    // Should fail validation
    assertEquals(errors.size, 1, "Should have 1 validation error")
    errors.head match
      case ValidationError.ContentError(_, _, msg, _) =>
        assert(msg.contains("Mermaid"), "Error should mention Mermaid requirement")
      case other =>
        fail(s"Expected ContentError, got: $other")

end TemplateContentValidationSpec
