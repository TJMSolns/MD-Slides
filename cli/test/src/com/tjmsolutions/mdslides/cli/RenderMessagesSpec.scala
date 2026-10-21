package com.tjmsolutions.mdslides.cli

/** MS-031: two CLI messages that told the user something false. */
class RenderMessagesSpec extends munit.FunSuite:

  test("the config hint gives a valid empty JSON object") {
    assertEquals(RenderMessages.createConfigCommand("/home/u/.mdslides/config.json"), "echo '{}' > /home/u/.mdslides/config.json")
  }

  test("a diagram that failed to render is not counted as pre-rendered") {
    assertEquals(RenderMessages.diagramSummary(total = 1, rendered = 0), Some("⚠ 1 diagram(s) will show their source with an install notice (install mermaid-cli for full support)"))
    assertEquals(RenderMessages.diagramSummary(total = 3, rendered = 2), Some("✓ Pre-rendered 2 of 3 diagram(s); 1 will show its source with an install notice"))
    assertEquals(RenderMessages.diagramSummary(total = 4, rendered = 4), Some("✓ Pre-rendered 4 diagram(s)"))
    assertEquals(RenderMessages.diagramSummary(total = 0, rendered = 0), None)
  }
