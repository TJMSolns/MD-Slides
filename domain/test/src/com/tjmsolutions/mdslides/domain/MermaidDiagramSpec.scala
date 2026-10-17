package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for Mermaid Diagram Support (v2.0.0).
 *
 * Tests verify:
 * - MermaidDiagram domain model
 * - Diagram type detection
 * - Alt text requirement for accessibility
 * - DiagramElement as part of ContentElement sealed trait
 *
 * Related Governance:
 * - v2.0.0: Mermaid Diagram Support (US-022)
 * - Example Mapping: Scenarios 1-7
 */
class MermaidDiagramSpec extends FunSuite:

  // Example Mapping Scenario 1: Create Mermaid diagram with flowchart source
  test("create Mermaid diagram with flowchart source"):
    val source = """graph TD
        A[Start] --> B{Decision}
        B -->|Yes| C[Good]
        B -->|No| D[Bad]"""

    val diagram = MermaidDiagram(source, diagramType = "flowchart", altText = Some("Flow showing decision process"))

    assertEquals(diagram.source, source)
    assertEquals(diagram.diagramType, "flowchart")
    assertEquals(diagram.altText, Some("Flow showing decision process"))

  // Example Mapping Scenario 2: Detect diagram type from source (flowchart)
  test("detect flowchart diagram type from 'graph TD' syntax"):
    val source = "graph TD\n    A --> B"

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "flowchart")

  // Example Mapping Scenario 3: Detect diagram type (sequence)
  test("detect sequence diagram type from 'sequenceDiagram' keyword"):
    val source = """sequenceDiagram
        Alice->>Bob: Hello Bob
        Bob-->>Alice: Hi Alice"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "sequence")

  // Example Mapping Scenario 4: Detect diagram type (class)
  test("detect class diagram type from 'classDiagram' keyword"):
    val source = """classDiagram
        class Animal {
            +String name
            +makeSound()
        }"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "class")

  // Example Mapping Scenario 5: Detect diagram type (state)
  test("detect state diagram type from 'stateDiagram' keyword"):
    val source = """stateDiagram-v2
        [*] --> Still
        Still --> Moving"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "state")

  // Example Mapping Scenario 6: Detect diagram type (gantt)
  test("detect gantt diagram type from 'gantt' keyword"):
    val source = """gantt
        title Project Timeline
        section Planning
        Task 1 :a1, 2025-01-01, 30d"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "gantt")

  // Example Mapping Scenario 7: Unknown diagram type defaults to "unknown"
  test("unknown diagram type defaults to 'unknown'"):
    val source = "invalid mermaid syntax"

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "unknown")

  // Additional diagram types
  test("detect pie diagram type from 'pie' keyword"):
    val source = """pie title Pie Chart
        "Dogs" : 42
        "Cats" : 50"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "pie")

  test("detect ER diagram type from 'erDiagram' keyword"):
    val source = """erDiagram
        CUSTOMER ||--o{ ORDER : places"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "er")

  test("detect user journey diagram type from 'journey' keyword"):
    val source = """journey
        title My working day
        section Go to work
          Make tea: 5: Me"""

    val diagramType = MermaidDiagram.detectDiagramType(source)

    assertEquals(diagramType, "journey")

  // Diagram without alt text (for accessibility validation)
  test("diagram without alt text has None"):
    val diagram = MermaidDiagram("graph TD\n    A --> B", diagramType = "flowchart", altText = None)

    assertEquals(diagram.altText, None)

  // Diagram with empty alt text (should be treated as None)
  test("diagram with empty alt text"):
    val diagram = MermaidDiagram("graph TD\n    A --> B", diagramType = "flowchart", altText = Some(""))

    assertEquals(diagram.altText, Some(""))
    assert(diagram.altText.exists(_.isEmpty), "Alt text should be empty string")

  // DiagramElement as ContentElement
  test("DiagramElement is a ContentElement"):
    val diagram = MermaidDiagram("graph TD\n    A --> B", "flowchart", None)
    val element: ContentElement = DiagramElement(diagram)

    element match
      case DiagramElement(d) =>
        assertEquals(d.diagramType, "flowchart")
      case _ =>
        fail("Expected DiagramElement")

end MermaidDiagramSpec
