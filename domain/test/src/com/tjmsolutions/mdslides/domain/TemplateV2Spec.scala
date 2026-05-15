package com.tjmsolutions.mdslides.domain

import munit.FunSuite

/**
 * Tests for Additional Templates (v2.0.0).
 *
 * Tests verify:
 * - New template definitions (diagram, closing, section-title)
 * - Template slot requirements
 * - Template resolution from name
 * - Backward compatibility (default to content)
 *
 * Related Governance:
 * - v2.0.0: Additional Templates
 * - Example Mapping: Scenarios 16-31
 */
class TemplateV2Spec extends FunSuite:

  // Example Mapping Scenario 16: Diagram template selection
  test("diagram template exists and can be resolved"):
    val result = Template.fromName("diagram")

    assert(result.isRight, "Diagram template should exist")
    val template = result.getOrElse(fail("Expected Right"))
    assertEquals(template.name, "diagram")

  // Example Mapping Scenario 17: Diagram template has heading and optional caption slots
  test("diagram template has required heading and optional caption"):
    val template = Template.fromName("diagram").getOrElse(fail("Diagram template should exist"))

    val heading = template.getSlot("heading")
    assert(heading.isDefined, "Diagram template should have heading slot")
    assert(heading.get.required, "Heading should be required")

    val caption = template.getSlot("caption")
    assert(caption.isDefined, "Diagram template should have caption slot")
    assert(!caption.get.required, "Caption should be optional")

  // Example Mapping Scenario 21: Closing template selection
  test("closing template exists and can be resolved"):
    val result = Template.fromName("closing")

    assert(result.isRight, "Closing template should exist")
    val template = result.getOrElse(fail("Expected Right"))
    assertEquals(template.name, "closing")

  // Example Mapping Scenario 24: Closing template has required heading and optional body
  test("closing template has required heading and optional body"):
    val template = Template.fromName("closing").getOrElse(fail("Closing template should exist"))

    val heading = template.getSlot("heading")
    assert(heading.isDefined, "Closing template should have heading slot")
    assert(heading.get.required, "Heading should be required")

    val body = template.getSlot("body")
    assert(body.isDefined, "Closing template should have body slot")
    assert(!body.get.required, "Body should be optional")

  // Example Mapping Scenario 25: Section title template selection
  test("section-title template exists and can be resolved"):
    val result = Template.fromName("section-title")

    assert(result.isRight, "Section-title template should exist")
    val template = result.getOrElse(fail("Expected Right"))
    assertEquals(template.name, "section-title")

  // Example Mapping Scenario 28: Section title has required heading and optional body (subtitle)
  test("section-title template has required heading and optional body"):
    val template = Template.fromName("section-title").getOrElse(fail("Section-title template should exist"))

    val heading = template.getSlot("heading")
    assert(heading.isDefined, "Section-title template should have heading slot")
    assert(heading.get.required, "Heading should be required")

    val body = template.getSlot("body")
    assert(body.isDefined, "Section-title template should have body slot")
    assert(!body.get.required, "Body should be optional for section-title")

  // Example Mapping Scenario 31: Invalid template name shows error
  test("invalid template name returns error"):
    val result = Template.fromName("invalid-name")

    assert(result.isLeft, "Invalid template should return Left")
    val error = result.left.getOrElse(fail("Expected error"))
    assert(error.contains("invalid-name"), s"Error should mention template name: $error")
    assert(error.contains("diagram"), s"Error should list valid templates: $error")
    assert(error.contains("closing"), s"Error should list valid templates: $error")
    assert(error.contains("section-title"), s"Error should list valid templates: $error")

  // Additional test: All v2.0.0 templates
  test("all v2.0.0 templates can be resolved"):
    val templateNames = List("title", "content", "diagram", "closing", "section-title")

    templateNames.foreach { name =>
      val result = Template.fromName(name)
      assert(result.isRight, s"Template '$name' should exist")
    }

  // Test required vs optional slots
  test("diagram template required slots"):
    val template = Template.fromName("diagram").getOrElse(fail("Expected diagram template"))
    val required = template.requiredSlots.map(_.name)

    assertEquals(required, List("heading"), "Diagram template should require only heading")

  test("closing template optional slots"):
    val template = Template.fromName("closing").getOrElse(fail("Expected closing template"))
    val optional = template.optionalSlots.map(_.name).toSet

    assert(optional.contains("body"), "Closing template should have optional body")

  test("section-title template slots"):
    val template = Template.fromName("section-title").getOrElse(fail("Expected section-title template"))

    assertEquals(template.requiredSlots.map(_.name), List("heading"))
    assert(template.optionalSlots.map(_.name).contains("body"))

end TemplateV2Spec
