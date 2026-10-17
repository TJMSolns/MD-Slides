package com.tjmsolutions.mdslides.domain

/**
 * Test suite for TemplateLayout value object
 *
 * Domain Model: doc/domain-models/aggregates/template-configuration-aggregate.md
 * Acceptance Criteria: doc/acceptance-criteria/template-configuration-acceptance-criteria.md (AC-3)
 */
class TemplateLayoutSpec extends munit.FunSuite:

  test("TemplateLayout should define two layout values") {
    val layouts = TemplateLayout.values
    assertEquals(layouts.length, 2)
    assertEquals(layouts.map(_.toString).toSet, Set("SingleColumn", "TwoColumn"))
  }

  test("fromString should parse 'single-column' case-insensitively") {
    assertEquals(TemplateLayout.fromString("single-column"), Right(TemplateLayout.SingleColumn))
    assertEquals(TemplateLayout.fromString("Single-Column"), Right(TemplateLayout.SingleColumn))
  }

  test("fromString should parse 'two-column' case-insensitively") {
    assertEquals(TemplateLayout.fromString("two-column"), Right(TemplateLayout.TwoColumn))
    assertEquals(TemplateLayout.fromString("Two-Column"), Right(TemplateLayout.TwoColumn))
  }

  test("fromString should reject invalid layout names") {
    assert(TemplateLayout.fromString("three-column").isLeft)
    assert(TemplateLayout.fromString("grid").isLeft)
  }

  test("SingleColumn should not require column config") {
    assertEquals(TemplateLayout.SingleColumn.requiresColumnConfig, false)
  }

  test("TwoColumn should require column config") {
    assertEquals(TemplateLayout.TwoColumn.requiresColumnConfig, true)
  }

  test("default layout should be SingleColumn") {
    assertEquals(TemplateLayout.default, TemplateLayout.SingleColumn)
  }
